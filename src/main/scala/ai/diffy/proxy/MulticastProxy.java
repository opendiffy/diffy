package ai.diffy.proxy;

import ai.diffy.analysis.DifferenceResult;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.Message;
import ai.diffy.functional.algebra.monoids.functions.BinaryOperator;
import ai.diffy.functional.algebra.monoids.functions.SeptaOperator;
import ai.diffy.functional.functions.Try;
import reactor.netty.http.server.HttpServerRequest;
import scala.Option;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MulticastProxy
        implements SeptaOperator<HttpServerRequest,
                HttpServerRequest, CompletableFuture<HttpMessage>,
                HttpServerRequest, CompletableFuture<HttpMessage>,
                HttpServerRequest, CompletableFuture<HttpMessage>,
                AnalysisRequest, CompletableFuture<Option<DifferenceResult>>,
                HttpRequest, Message,
                HttpResponse, Message,
                Void, Integer,
                CompletableFuture<HttpResponse>> {

    @Override
    public Function<HttpServerRequest, CompletableFuture<HttpResponse>> apply(
            Function<HttpServerRequest, CompletableFuture<HttpMessage>> primary,
            Function<HttpServerRequest, CompletableFuture<HttpMessage>> secondary,
            Function<HttpServerRequest, CompletableFuture<HttpMessage>> candidate,
            Function<AnalysisRequest, CompletableFuture<Option<DifferenceResult>>> analyzer,
            Function<HttpRequest, Message> liftRequest,
            Function<HttpResponse, Message> liftResponse,
            Function<Void, Integer> indexSupplier) {

        Function<HttpServerRequest, CompletableFuture<HttpMessage>[]> tricast =
                tricastOperator.apply(primary, secondary, candidate);

        Function<CompletableFuture<HttpMessage>[], CompletableFuture<Try<AnalysisRequest>>> lift =
                lifterOperator.apply(liftRequest, liftResponse);

        Function<HttpServerRequest, CompletableFuture<HttpResponse>> proxy =
                (HttpServerRequest request) -> {
                    CompletableFuture<HttpMessage>[] responses = tricast.apply(request);
                    CompletableFuture<Try<AnalysisRequest>> analysisRequest = lift.apply(responses);
                    analysisRequest.thenAccept(tryRequest -> tryRequest.map(analyzer));
                    return responses[indexSupplier.apply(null)].thenApply(r -> (HttpResponse)r);
                };
        return proxy;
    }

    private interface HttpTricastOperator extends TricastFunctionOperator<HttpServerRequest, CompletableFuture<HttpMessage>> {}
    private static HttpTricastOperator tricastOperator =
        (primary, secondary, candidate) -> (HttpServerRequest req) -> {

        CompletableFuture<HttpMessage> request = new CompletableFuture<>();
        req.receive().aggregate().asString().toFuture().thenAccept(body -> {
            request.complete(new HttpRequest(
                req.method().name(),
                req.uri(),
                req.path(),
                req.params(),
                req.requestHeaders(),
                body
            ));
        });
        CompletableFuture<HttpMessage>[] messages = new CompletableFuture[4];
        messages[0] = primary.apply(req);
        messages[1] = messages[0]
                .thenCompose((success) -> candidate.apply(req))
                .exceptionallyCompose((error) -> candidate.apply(req));
        messages[2] = messages[1]
                .thenCompose((success) -> secondary.apply(req))
                .exceptionallyCompose((error) -> secondary.apply(req));
        messages[3] = request;
        return messages;
    };

    private interface LifterOperator extends BinaryOperator<CompletableFuture<HttpMessage>[],
                    HttpRequest, Message,
                    HttpResponse, Message,
                    CompletableFuture<Try<AnalysisRequest>>> {}

    private static LifterOperator lifterOperator = (liftRequest, liftResponse) -> (messages) ->
        messages[2].thenApply(msgS -> Try.of(() -> {
            Message r = liftRequest.apply((HttpRequest) messages[3].get());
            Message c = liftResponse.apply((HttpResponse) messages[1].get());
            Message p = liftResponse.apply((HttpResponse) messages[0].get());
            Message s = liftResponse.apply((HttpResponse) messages[2].get());
            return AnalysisRequest.apply(r, c, p, s);
        }));
}
