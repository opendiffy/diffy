package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.DependentEndpoint;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.functions.Try;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * AsyncEndpointWrapper wraps a synchronous NamedEndpoint with a ForkJoinPool to produce an async interface
 * @param <Request>
 * @param <Response>
 */
public class Async<Request, Response> extends DependentEndpoint<Request, Request, Response, CompletableFuture<Response>> {
    public static <Request, Response> Async<Request, Response> common(Endpoint<Request, Response> dependency) {
        return new Async<>(ForkJoinPool.commonPool(), dependency);
    }
    public Async(ForkJoinPool pool, Endpoint<Request, Response> dependency) {
        super(dependency.getName()+".async", dependency, applyDependency -> (Request request) -> {
            CompletableFuture<Response> result = new CompletableFuture<>();
            pool.execute(()-> {
                Try<Boolean> x = Try.of(() -> result.complete(applyDependency.apply(request)));
                x.isNormal();
            });
            return result;
        });
    }
}
