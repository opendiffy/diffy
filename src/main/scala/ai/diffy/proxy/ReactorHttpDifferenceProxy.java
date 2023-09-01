package ai.diffy.proxy;

import ai.diffy.Settings;
import ai.diffy.analysis.*;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.endpoints.SeptaDependentEndpoint;
import ai.diffy.functional.topology.Async;
import ai.diffy.functional.topology.ControlFlowLogger;
import ai.diffy.functional.topology.InvocationLogger;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.HttpLifter;
import ai.diffy.repository.DifferenceResultRepository;
import ai.diffy.transformations.TransformationCachingService;
import ai.diffy.transformations.TransformationEdge;
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
import ai.diffy.functional.algebra.monoids.functions.BinaryOperator;
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
    public final TransformationCachingService transformations;
    volatile public Date lastReset = new Date();

    private final Endpoint<HttpServerRequest,CompletableFuture<HttpResponse>> multicastProxy;
    private final Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> loggedMulticastProxy;

    private final Endpoint<HttpRequest, CompletableFuture<HttpResponse>> primary;
    private final Endpoint<HttpRequest, CompletableFuture<HttpResponse>> secondary;
    private final Endpoint<HttpRequest, CompletableFuture<HttpResponse>> candidate;
    private final Endpoint<AnalysisRequest, CompletableFuture<Option<DifferenceResult>>> analyzer;

    private final HttpLifter lifter;
    private final Function<
            Tuple3<
                    CompletableFuture<HttpResponse>,
                    CompletableFuture<HttpResponse>,
                    CompletableFuture<HttpResponse>
                    >,
            CompletableFuture<HttpResponse>
            > responsePicker;
    private static CompletableFuture<HttpResponse> None =
            CompletableFuture.completedFuture(new HttpResponse(HttpResponseStatus.OK.toString(), EmptyHttpHeaders.INSTANCE,""));
    public ReactorHttpDifferenceProxy(
            @Autowired Settings settings,
            @Autowired DifferenceResultRepository repository,
            @Autowired TransformationCachingService transformations) {
        this.settings = settings;
        this.transformations = transformations;
        this.collector = new InMemoryDifferenceCollector();
        RawDifferenceCounter raw = RawDifferenceCounter.apply(new InMemoryDifferenceCounter("raw"));
        NoiseDifferenceCounter noise = NoiseDifferenceCounter.apply(new InMemoryDifferenceCounter("noise"));
        this.joinedDifferences = JoinedDifferences.apply(raw,noise);
        this.responsePicker = t3 -> {
            switch (settings.responseMode()) {
                case primary : return t3.getT1();
                case candidate : return t3.getT2();
                case secondary : return t3.getT3();
                case none : return None;
            }
            return None;
        };

        this.lifter = new HttpLifter(settings);
        log.info("Starting Proxy server on port "+ settings.servicePort());

        /**
         * Let's build a topology
         */
        primary = Async.common(HttpEndpoint.from(
                "primary",
                settings.primary()
        ));
        secondary = Async.common(HttpEndpoint.from(
                "secondary",
                settings.secondary()
        ));
        candidate = Async.common(HttpEndpoint.from(
                "candidate", settings.candidate()
        ));
        analyzer = Async.common(Endpoint.from(
            "analyzerWithRepo",
                Endpoint.from("analyzer", () -> new DifferenceAnalyzer(raw, noise, collector)::analyze),
                Endpoint.from("repo", () -> repository::save),
                (BinaryOperator<AnalysisRequest,
                        AnalysisRequest, Option<DifferenceResult>,
                        DifferenceResult, DifferenceResult,
                        Option<DifferenceResult>>) (analyzerLambda, repoSave) -> analyzerLambda.andThen(ro -> ro.map(r -> repoSave.apply(r)))
        ));
        multicastProxy =
            Endpoint.from(
                "buffer",
                HttpEndpoint.RequestBuffer,
                Endpoint.from(
                    "proxy",
                    primary,
                    secondary,
                    candidate,
                    analyzer,
                    Endpoint.from("liftRequest", ()-> lifter::liftRequest),
                    Endpoint.from("liftResponse", ()-> lifter::liftResponse),
                    Endpoint.from("responsePicker", () -> responsePicker),
                    MulticastProxy.Operator
                ),
                (buffer, proxy) -> buffer.andThen(proxy)
            );
        /**
         * Now that our topology is ready let's install some filter middleware.
         * In this example we will install an InvocationLogger that monitors
         * the beginning and end of a NameEndpoint invocation
         */

        loggedMulticastProxy = multicastProxy;
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
        Endpoint<HttpRequest, CompletableFuture<HttpResponse>> transformedPrimary = transformations.apply(TransformationEdge.primary, primary);
        Endpoint<HttpRequest, CompletableFuture<HttpResponse>> transformedSecondary = transformations.apply(TransformationEdge.secondary, secondary);
        Endpoint<HttpRequest, CompletableFuture<HttpResponse>> transformedCandidate = transformations.apply(TransformationEdge.candidate, candidate);

        SeptaDependentEndpoint all = Endpoint.from(
                "proxy",
                transformedPrimary,
                transformedSecondary,
                transformedCandidate,
                analyzer,
                Endpoint.from("liftRequest", () -> lifter::liftRequest),
                Endpoint.from("liftResponse", () -> lifter::liftResponse),
                Endpoint.from("responsePicker", () -> responsePicker),
                MulticastProxy.Operator
        );

        Endpoint<HttpServerRequest, CompletableFuture<HttpResponse>> perRequestEndpoint = Endpoint.from(
                "buffer",
                HttpEndpoint.RequestBuffer,
                transformations.apply(TransformationEdge.all, all),
                (Function<HttpServerRequest, CompletableFuture<HttpRequest>> buffer,
                 Function<HttpRequest, CompletableFuture<HttpResponse>> proxy) ->
                        (HttpServerRequest srvReq) -> buffer.apply(srvReq).thenCompose(proxy::apply)
        );

        ControlFlowLogger controlFlow = new ControlFlowLogger();
        
        return Mono.fromFuture(
                perRequestEndpoint.apply(req).whenComplete((response, t)->{
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
