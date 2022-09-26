package ai.diffy.proxy;

import ai.diffy.Settings;
import ai.diffy.analysis.*;
import ai.diffy.lifter.HttpLifter;
import ai.diffy.lifter.JsonLifter;
import ai.diffy.lifter.Message;
import ai.diffy.repository.DifferenceResultRepository;
import io.netty.handler.codec.http.HttpMethod;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.extension.annotations.WithSpan;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReactorHttpDifferenceProxy {
    private final Logger log = LoggerFactory.getLogger(ReactorHttpDifferenceProxy.class);
    private DisposableServer server;
    private HttpClient primary;
    private HttpClient secondary;
    private HttpClient candidate;
    private int responseIndex;

    private final Settings settings;
    private final DifferenceAnalyzer analyzer;
    public final JoinedDifferences joinedDifferences;
    public final InMemoryDifferenceCollector collector;
    private final HttpLifter lifter;

    volatile public Date lastReset = new Date();

    public ReactorHttpDifferenceProxy(@Autowired Settings settings, @Autowired DifferenceResultRepository repository) {
        this.settings = settings;
        this.collector = new InMemoryDifferenceCollector();
        RawDifferenceCounter raw = RawDifferenceCounter.apply(new InMemoryDifferenceCounter("raw"));
        NoiseDifferenceCounter noise = NoiseDifferenceCounter.apply(new InMemoryDifferenceCounter("noise"));
        this.joinedDifferences = JoinedDifferences.apply(raw,noise);
        this.analyzer = new DifferenceAnalyzer(raw, noise, collector, repository);
        this.lifter = new HttpLifter(settings);

        log.info("Starting Proxy server on port "+ settings.servicePort());
        primary = HttpClient.create()
                .host(settings.primaryHost())
                .port(settings.primaryPort());
        secondary = HttpClient.create()
                .host(settings.secondaryHost())
                .port(settings.secondaryPort());
        candidate = HttpClient.create()
                .host(settings.candidateHost())
                .port(settings.candidatePort());
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
        CompletableFuture<HttpRequest> request = new CompletableFuture<>();
        req.receive().aggregate().asString().toFuture().thenAccept(body -> {
            request.complete(new HttpRequest(
                    req.method().name(),
                    req.uri(),
                    req.path(),
                    req.params(),
                    new HttpMessage(req.requestHeaders(), body)
            ));
        });

        CompletableFuture<HttpResponse>[] messages = new CompletableFuture[]{
            new CompletableFuture<>(),
            new CompletableFuture<>(),
            new CompletableFuture<>()
        };

        receive(primary, req, "primary").thenAccept(messages[0]::complete);
        messages[0].thenAccept(msgP -> { receive(candidate, req, "candidate").thenAccept(messages[1]::complete);});
        messages[1].thenAccept(msgC -> { receive(secondary, req, "secondary").thenAccept(messages[2]::complete);});
        messages[2].thenAccept(msgS -> {
            try {
                Message r = lifter.liftRequest(request.get());
                Message c = lifter.liftResponse(messages[1].get());
                Message p = lifter.liftResponse(messages[0].get());
                Message s = lifter.liftResponse(messages[2].get());
                analyze(r, c, p, s);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

        return Mono.fromFuture(messages[responseIndex])
                .flatMap(r -> res.headers(r.getMessage().headers).sendString(Mono.just(r.getMessage().body)).then());
    }

    @WithSpan
    private void analyze(Message r, Message c, Message p, Message s) {
        final Span span = Span.current();
        analyzer.apply(r, c, p, s).foreach(diffResult ->
            span.addEvent("DifferenceResult", Attributes.of(
                AttributeKey.stringKey("endpoint"), diffResult.endpoint(),
                AttributeKey.stringKey("request"), diffResult.request(),
                AttributeKey.stringKey("candidate"), diffResult.responses().candidate(),
                AttributeKey.stringKey("primary"), diffResult.responses().primary(),
                AttributeKey.stringKey("secondary"), diffResult.responses().secondary()
            ))
        );
    }
    private Mono<HttpResponse> receiveMono(HttpClient client, HttpServerRequest req) {
        return client.request(req.method())
                .uri(req.uri())
                .send(req.receive().retain())
                .responseSingle(
                        (headers, body) ->
                                body.asString()
                                        .map(b -> new HttpMessage(headers.responseHeaders(), b))
                                        .map(m -> new HttpResponse(headers.status().toString(), m))
                );
    }
    private ForkJoinPool pool = ForkJoinPool.commonPool();
    private CompletableFuture<HttpResponse> receive(HttpClient client, HttpServerRequest req, String name) {
        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        pool.execute(() -> receive(client, req, name, result));
        return result;
    }

//    @WithSpan
    private void receive(HttpClient client, HttpServerRequest req, String name, CompletableFuture<HttpResponse> result) {
        result.complete(receiveMono(client, req).block());
//        try {
//            Span span = Span.current();
//            log.info("Receive {} {}.{}", name, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
//            span.addEvent(name, Attributes.of(AttributeKey.stringKey("response"), JsonLifter.encode(lifter.liftResponse(result.get()))));
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
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
        analyzer.clear();
    }

}
