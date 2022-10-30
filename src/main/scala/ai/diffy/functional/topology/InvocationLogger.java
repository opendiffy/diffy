package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.Endpoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class InvocationLogger{
    private static final AtomicInteger invocationCount = new AtomicInteger(0);

    public static final <Request, Response> Endpoint<Request, Response> mapper(Endpoint<Request, Response> e, List<Endpoint> d) {
        return e.andThenMiddleware((apply) -> (request) -> {
            String name = e.getName();
            final int invocationInstance = invocationCount.getAndIncrement();
            System.out.println(name + " starting [" + invocationInstance + "]");
            final long start = System.currentTimeMillis();
            Response result = (Response) apply.apply(request);
            if (CompletableFuture.class.isAssignableFrom(result.getClass())) {
                ((CompletableFuture) result).whenComplete((response, error) -> {
                    System.out.println(name + " completed async [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
                });
            } else {
                System.out.println(name + " finished sync [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
            }
            return result;
        }).withDownstream(d);
    }
}
