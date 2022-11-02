package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.DependentEndpoint;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.functions.Try;
import ai.diffy.util.Future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * AsyncEndpointWrapper wraps a synchronous NamedEndpoint with a ForkJoinPool to produce an async interface
 * @param <Request>
 * @param <Response>
 */
public class Async<Request, Response> extends DependentEndpoint<Request, Request, Response, CompletableFuture<Response>> {
    public static <Request, Response> Function<Request, CompletableFuture<Response>> operator(ForkJoinPool pool, Function<Request, Response> applyDependency) {
        return (Request request) -> {
            CompletableFuture<Response> result = new CompletableFuture<>();
            pool.execute(()-> {
                Try<Boolean> x = Try.of(() -> result.complete(applyDependency.apply(request)));
                if(!x.isNormal()){
                    result.completeExceptionally(x.getThrowable());
                }
                assert result.isDone();
            });
            return result;
        };
    }
    public static <Request, Response> Async<Request, Response> common(Endpoint<Request, Response> dependency) {
        return new Async<>(ForkJoinPool.commonPool(), dependency);
    }
    public static <Request, Response> Endpoint<Request, CompletableFuture<Response>> contain(Endpoint<Request, CompletableFuture<Response>> dependency) {
        return contain(ForkJoinPool.commonPool(), dependency);
    }
        public static <Request, Response> Endpoint<Request, CompletableFuture<Response>> contain(ForkJoinPool pool, Endpoint<Request, CompletableFuture<Response>> dependency) {
        return Endpoint.from(
            dependency.getName()+".contained",
            () -> (Request request) -> {
                final CompletableFuture<Response> result = new CompletableFuture<>();
                pool.execute(()-> {
                    Future.assign(
                        Try.of(() -> dependency.apply(request))
                            .toFuture()
                            .thenCompose(Function.identity()),
                        result
                    );
                });
                return result;
            });
    }
    public Async(ForkJoinPool pool, Endpoint<Request, Response> dependency) {
        super(dependency.getName()+".async", dependency, applyDependency -> operator(pool, applyDependency));
    }
}
