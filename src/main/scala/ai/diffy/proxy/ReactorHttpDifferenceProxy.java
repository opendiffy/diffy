package ai.diffy.proxy;

import ai.diffy.Settings;
import ai.diffy.analysis.*;
import ai.diffy.functional.topology.ControlFlowLogger;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.HttpLifter;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.topology.Async;
import ai.diffy.functional.topology.InvocationLogger;
import ai.diffy.repository.DifferenceResultRepository;
import ai.diffy.interpreter.Transformer;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import reactor.util.function.Tuple3;
import scala.Option;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReactorHttpDifferenceProxy {
    private final Logger log = LoggerFactory.getLogger(ReactorHttpDifferenceProxy.class);
    private DisposableServer server;
    private final Settings settings;
    public final JoinedDifferences joinedDifferences;
    public final InMemoryDifferenceCollector collector;
    volatile public Date lastReset = new Date();

    private Endpoint<HttpServerRequest,CompletableFuture<HttpResponse>> multicastProxy;
    private Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> loggedMulticastProxy;
    private static CompletableFuture<HttpResponse> None =
            CompletableFuture.completedFuture(new HttpResponse(HttpResponseStatus.OK.toString(), EmptyHttpHeaders.INSTANCE,""));
    public ReactorHttpDifferenceProxy(@Autowired Settings settings, @Autowired DifferenceResultRepository repository) {
        this.settings = settings;
        this.collector = new InMemoryDifferenceCollector();
        RawDifferenceCounter raw = RawDifferenceCounter.apply(new InMemoryDifferenceCounter("raw"));
        NoiseDifferenceCounter noise = NoiseDifferenceCounter.apply(new InMemoryDifferenceCounter("noise"));
        this.joinedDifferences = JoinedDifferences.apply(raw,noise);
        Function<
                Tuple3<
                        CompletableFuture<HttpResponse>,
                        CompletableFuture<HttpResponse>,
                        CompletableFuture<HttpResponse>
                >,
                CompletableFuture<HttpResponse>
        > responsePicker = t3 -> switch (settings.responseMode()) {
            case primary -> t3.getT1();
            case candidate -> t3.getT2();
            case secondary -> t3.getT3();
            case none -> None;
        };

        HttpLifter lifter = new HttpLifter(settings);
        log.info("Starting Proxy server on port "+ settings.servicePort());

        /**
         * Let's build a topology
         */
        Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> primary = Async.common(HttpEndpoint.from(
                "primary",
                settings.primaryHost(), settings.primaryPort()
        ));
        Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> secondary = Async.common(HttpEndpoint.from(
                "secondary",
                settings.secondaryHost(), settings.secondaryPort()
        ));
        Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> candidate = Async.common(HttpEndpoint.from(
                "candidate", settings.candidateHost(), settings.candidatePort()
        ));
        Endpoint<AnalysisRequest, CompletableFuture<Option<DifferenceResult>>> analyzer = Async.common(Endpoint.from(
            "analyzer",
            () -> new DifferenceAnalyzer(raw, noise, collector, repository)::analyze
        ));
        multicastProxy = Endpoint.from(
                "proxy",
                primary,
                secondary,
                candidate,
                analyzer,
                Endpoint.from("liftRequest", ()-> lifter::liftRequest),
                Endpoint.from("liftResponse", ()-> lifter::liftResponse),
                Endpoint.from("responsePicker", () -> responsePicker),
                MulticastProxy.Operator);
        /**
         * Now that our topology is ready let's install some filter middleware.
         * In this example we will install an InvocationLogger that monitors
         * the beginning and end of a NameEndpoint invocation
         */
        Transformer<HttpResponse> responseTx =
                new Transformer<>(
                    HttpResponse.class,
                    "(r)=>{console.log(`proxy middleware: sending ${JSON.stringify(r)} response`);return r;}"
                );
        loggedMulticastProxy = multicastProxy
                .andThenMiddleware(next -> (req) -> next.apply(req).thenApply(responseTx.suppressThrowable()))
        ;
        /**
         * All set. We should be able to see InvocationLogger messages in the logs now.
         */
        server = HttpServer.create()
                .port(settings.servicePort())
                .handle(this::selectHandler)
                .bindNow();
    }
    private Publisher<Void> selectHandler(HttpServerRequest req, HttpServerResponse res) {
        if(!settings.allowHttpSideEffects() && methodsWithSideEffects.contains(req.method())){
            log.info("Ignoring {} request for safety. Use --allowHttpSideEffects=true to turn safety off.", req.method());
            return res.send();
        }
        ControlFlowLogger controlFlow = new ControlFlowLogger();
        return Mono.fromFuture(
            loggedMulticastProxy
                    //We can even transform the endpoint on a per request basis
                .deepTransform(controlFlow::mapper)
                .apply(req).whenComplete((response, t)->{
                    if(t != null) {
                        log.error("request failed to get response",t);
                        res
                        .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                        .sendString(Mono.just(t.getMessage())).then();
                    }
                    log.info(controlFlow.toString());
                }))
                .flatMap(r ->
                    res
                    .status(HttpResponseStatus.parseLine(r.getStatus()))
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
