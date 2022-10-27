package ai.diffy.functional.topology;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public interface AsyncCommonPoolUnaryOperator<Request, Response> extends AsyncUnaryOperator<Request, Response> {
    ForkJoinPool pool = ForkJoinPool.commonPool();
    @Override
    default Function<Request, CompletableFuture<Response>> apply(Function<Request, Response> dependency) {
        return (Request request) -> {
            CompletableFuture<Response> result = new CompletableFuture<>();
            pool.execute(() -> result.complete(dependency.apply(request)));
            return result;
        };
    }
}
