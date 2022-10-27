package ai.diffy.functional.topology;

import ai.diffy.functional.algebra.monoids.functions.UnaryOperator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface AsyncUnaryOperator<Request, Response> extends UnaryOperator<Request, Request, Response, CompletableFuture<Response>> {
    @Override
    default Function<Request, CompletableFuture<Response>> apply(Function<Request, Response> dependency) {
        return request -> CompletableFuture.completedFuture(dependency.apply(request));
    }
}
