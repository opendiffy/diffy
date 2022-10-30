package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.algebra.monoids.functions.SymmetricUnaryOperator;
import ai.diffy.functional.endpoints.IndependentEndpoint;
import ai.diffy.functional.endpoints.SymmetricDependentEndpoint;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InvocationLogger{
    private static final AtomicInteger invocationCount = new AtomicInteger(0);

    public static <Request, Response> Endpoint<Request, Response> wrap(Endpoint<Request, Response> endpoint){
        Set<Endpoint> deepDown = deepDownstream(endpoint);
        deepDown.forEach(deepDependency -> {
            System.out.println(deepDependency.getName());
        });

        ConcurrentHashMap<Endpoint, Endpoint> replacements = new ConcurrentHashMap<>(deepDown.size());
        Endpoint<Request, Response> rebuiltEndpoint = rebuild(endpoint, replacements);
        return rebuiltEndpoint;
    }

    private static <Request, Response> Endpoint<Request, Response> rebuild(Endpoint<Request, Response> endpoint, ConcurrentHashMap<Endpoint, Endpoint> replacements){
        if(!replacements.containsKey(endpoint)){
            Endpoint<Request, Response> wrappedEndpoint = new SymmetricDependentEndpoint<>(
                    endpoint.getName() + ".log",
                    endpoint.withDownstream(
                            endpoint.getDownstream().stream()
                                    .map(dependency -> rebuild(dependency, replacements))
                                    .toList()
                    ),
                    InvocationLogger.wrapApply(endpoint)
            );
            replacements.putIfAbsent(endpoint, wrappedEndpoint);
        }
        return replacements.get(endpoint);
    }
    private static <Request, Response> Set<Endpoint> deepDownstream(Endpoint<Request, Response> endpoint){
        return (Set<Endpoint>) Stream.concat(
                Stream.of(endpoint),
                endpoint.getDownstream().stream()
                .map(InvocationLogger::deepDownstream)
                .flatMap(Set::stream)
        ).collect(Collectors.toSet());
    }
    private static <Request, Response> SymmetricUnaryOperator<Request, Response> wrapApply(Endpoint<Request, Response> endpoint){
        if(!(endpoint instanceof IndependentEndpoint)) {
            return apply -> apply;
        }
        String name = endpoint.getName();
        System.out.println("InvocationLogger wrapping " + name);
        return (apply) -> (request) -> {
            final int invocationInstance = invocationCount.getAndIncrement();
            System.out.println(name + " starting [" + invocationInstance + "]");
            final long start = System.currentTimeMillis();
            Response result = (Response) apply.apply(request);
            if (CompletableFuture.class.isAssignableFrom(result.getClass())) {
                ((CompletableFuture) result).thenAccept((response) -> {
                    System.out.println(name + " completed async [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
                });
            } else {
                System.out.println(name + " finished sync [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
            }
            return result;
        };
    }

    public static final <Request, Response> Endpoint<Request, Response> mapper(Endpoint<Request, Response> e, List<Endpoint> d) {
        return e.andThenMiddleware((apply) -> (request) -> {
            String name = e.getName();
            final int invocationInstance = invocationCount.getAndIncrement();
            System.out.println(name + " starting [" + invocationInstance + "]");
            final long start = System.currentTimeMillis();
            Response result = (Response) apply.apply(request);
            if (CompletableFuture.class.isAssignableFrom(result.getClass())) {
                ((CompletableFuture) result).thenAccept((response) -> {
                    System.out.println(name + " completed async [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
                });
            } else {
                System.out.println(name + " finished sync [" + invocationInstance + "] in " + (System.currentTimeMillis() - start));
            }
            return result;
        }).withDownstream(d);
    }
}
