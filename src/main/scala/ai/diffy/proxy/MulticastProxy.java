package ai.diffy.proxy;

import ai.diffy.analysis.DifferenceResult;
import ai.diffy.functional.algebra.monoids.functions.OctaOperator;
import ai.diffy.functional.algebra.monoids.functions.SeptaOperator;
import ai.diffy.functional.functions.Try;
import ai.diffy.lifter.AnalysisRequest;
import ai.diffy.lifter.Message;
import ai.diffy.util.Future;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import scala.Option;

import java.util.concurrent.CompletableFuture;

public class MulticastProxy {
    public static final SeptaOperator<HttpRequest,
            HttpRequest, CompletableFuture<HttpResponse>,
            HttpRequest, CompletableFuture<HttpResponse>,
            HttpRequest, CompletableFuture<HttpResponse>,
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
            ) -> (HttpRequest request) -> {
                CompletableFuture<HttpResponse> pr = primary.apply(request);
                CompletableFuture<HttpResponse> cr = Future.getAfter(pr, () -> candidate.apply(request));
                CompletableFuture<HttpResponse> sr = Future.getAfter(cr, () -> secondary.apply(request));

                sr.thenApply(msgS -> Try.of(() -> {
                    Message r = liftRequest.apply(request);
                    Message c = liftResponse.apply(cr.get());
                    Message p = liftResponse.apply(pr.get());
                    Message s = liftResponse.apply(sr.get());
                    return AnalysisRequest.apply(r, c, p, s);
                })).thenApply(tryRequest -> tryRequest.map(analyzer));
                return responsePicker.apply(Tuples.of(pr, cr, sr));
            };
}
