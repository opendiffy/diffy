package ai.diffy.proxy;

import ai.diffy.analysis.DifferenceResult;
import ai.diffy.functional.algebra.monoids.functions.SeptaOperator;
import ai.diffy.functional.functions.Try;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.Message;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import scala.Option;

import java.util.concurrent.CompletableFuture;

public class MulticastProxy {
    public static final SeptaOperator<HttpServerRequest,
            HttpServerRequest, CompletableFuture<HttpResponse>,
            HttpServerRequest, CompletableFuture<HttpResponse>,
            HttpServerRequest, CompletableFuture<HttpResponse>,
            AnalysisRequest, CompletableFuture<Option<DifferenceResult>>,
            HttpRequest, Message,
            HttpResponse, Message,
            Tuple3<CompletableFuture<HttpResponse>, CompletableFuture<HttpResponse>, CompletableFuture<HttpResponse>>, CompletableFuture<HttpResponse>,
            CompletableFuture<HttpResponse>> Operator =
            (
                    primary,
                    secondary,
                    candidate,
                    analyzer,
                    liftRequest,
                    liftResponse,
                    responsePicker
            ) -> (HttpServerRequest req) -> {
                CompletableFuture<HttpRequest> request = new CompletableFuture<>();
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

                CompletableFuture<HttpResponse> pr = primary.apply(req);
                CompletableFuture<HttpResponse> cr = pr
                        .thenCompose((success) -> candidate.apply(req))
                        .exceptionallyCompose((error) -> candidate.apply(req));
                CompletableFuture<HttpResponse>sr = cr
                        .thenCompose((success) -> secondary.apply(req))
                        .exceptionallyCompose((error) -> secondary.apply(req));

                sr.thenApply(msgS -> Try.of(() -> {
                    Message r = liftRequest.apply(request.get());
                    Message c = liftResponse.apply(cr.get());
                    Message p = liftResponse.apply(pr.get());
                    Message s = liftResponse.apply(sr.get());
                    return AnalysisRequest.apply(r, c, p, s);
                })).thenApply(tryRequest -> tryRequest.map(analyzer));
                return responsePicker.apply(Tuples.of(pr, cr, sr));
            };
}
