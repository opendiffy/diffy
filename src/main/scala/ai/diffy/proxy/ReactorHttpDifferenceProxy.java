package ai.diffy.proxy;

import ai.diffy.Settings;
import ai.diffy.analysis.*;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.HttpLifter;
import ai.diffy.lifter.Message;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.endpoints.IndependentEndpoint;
import ai.diffy.functional.endpoints.SeptaDependentEndpoint;
import ai.diffy.functional.topology.Async;
import ai.diffy.functional.topology.SpanWrapper;
import ai.diffy.functional.topology.InvocationLogger;
import ai.diffy.repository.DifferenceResultRepository;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import scala.Option;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReactorHttpDifferenceProxy {
    private final Logger log = LoggerFactory.getLogger(ReactorHttpDifferenceProxy.class);
    private DisposableServer server;
    private Endpoint<HttpServerRequest, CompletableFuture<HttpMessage>> primary;
    private Endpoint<HttpServerRequest, CompletableFuture<HttpMessage>> secondary;
    private Endpoint<HttpServerRequest, CompletableFuture<HttpMessage>> candidate;
    private SeptaDependentEndpoint<HttpServerRequest,
        HttpServerRequest, CompletableFuture<HttpMessage>,
        HttpServerRequest, CompletableFuture<HttpMessage>,
        HttpServerRequest, CompletableFuture<HttpMessage>,
        AnalysisRequest, CompletableFuture<Option<DifferenceResult>>,
        HttpRequest, Message,
        HttpResponse, Message,
        Void, Integer,
        CompletableFuture<HttpResponse>> multicastProxy;
    private Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> loggedMulticastProxy;
    private int responseIndex;
    private final Settings settings;
    private final Endpoint<AnalysisRequest, CompletableFuture<Option<DifferenceResult>>> analyzer;
    public final JoinedDifferences joinedDifferences;
    public final InMemoryDifferenceCollector collector;
    volatile public Date lastReset = new Date();

    public ReactorHttpDifferenceProxy(@Autowired Settings settings, @Autowired DifferenceResultRepository repository) {
        this.settings = settings;
        this.collector = new InMemoryDifferenceCollector();
        RawDifferenceCounter raw = RawDifferenceCounter.apply(new InMemoryDifferenceCounter("raw"));
        NoiseDifferenceCounter noise = NoiseDifferenceCounter.apply(new InMemoryDifferenceCounter("noise"));
        this.joinedDifferences = JoinedDifferences.apply(raw,noise);

        this.analyzer = Async.common(
            new IndependentEndpoint<>(
                "analyzer",
                () -> new DifferenceAnalyzer(raw, noise, collector, repository)::analyze
            )
        );

        HttpLifter lifter = new HttpLifter(settings);
        log.info("Starting Proxy server on port "+ settings.servicePort());

        /**
         * Let's build a topology
         */
        primary = Async.common(HttpEndpoint.from("primary", settings.primaryHost(), settings.primaryPort()));
        secondary = Async.common(HttpEndpoint.from("secondary", settings.secondaryHost(), settings.secondaryPort()));
        candidate = Async.common(HttpEndpoint.from("candidate", settings.candidateHost(), settings.candidatePort()));
        multicastProxy = Endpoint.from(
                "proxy",
                primary,
                secondary,
                candidate,
                analyzer,
                new IndependentEndpoint<>("liftRequest", ()-> lifter::liftRequest),
                new IndependentEndpoint<>("liftResponse", ()-> lifter::liftResponse),
                new IndependentEndpoint<>("responsePicker", () -> (x) -> responseIndex),
                new MulticastProxy());
        //(, responseIndex, primary, secondary, candidate, analyzer);
        /**
         * Now that our topology is ready let's install some filter middleware.
         * In this example we will install an InvocationLogger that monitors
         * the beginning and end of a NameEndpoint invocation
         */
//        loggedMulticastProxy = InvocationLogger.wrap(multicastProxy);
        loggedMulticastProxy = multicastProxy.deepTransform(InvocationLogger::mapper);
        /**
         * All set. We should be able to see InvocationLogger messages in the logs now.
         */
        server = HttpServer.create()
                .port(settings.servicePort())
                .handle(this::selectHandler)
                .bindNow();
        switch (settings.responseMode().name()) {
            case "candidate" : responseIndex = 1; break;
            case "secondary" : responseIndex = 2; break;
            default: responseIndex = 0;
        }
    }
    private Publisher<Void> selectHandler(HttpServerRequest req, HttpServerResponse res) {
        if(!settings.allowHttpSideEffects() && methodsWithSideEffects.contains(req.method())){
            log.info("Ignoring {} request for safety. Use --allowHttpSideEffects=true to turn safety off.", req.method());
            return res.send();
        }
        return Mono.fromFuture(loggedMulticastProxy.apply(req))
                .flatMap(r ->
                    res
                    .headers(HttpMessage.toHttpHeaders(r.headers))
                    .sendString(Mono.just(r.body))
                    .then()
                );
    }


    private static Set<HttpMethod> methodsWithSideEffects =
            Stream.of(
                    HttpMethod.POST,
                    HttpMethod.PUT,
                    HttpMethod.PATCH,
                    HttpMethod.DELETE
            ).collect(Collectors.toSet());

    public void clear(){
        lastReset = new Date();
    }

}
