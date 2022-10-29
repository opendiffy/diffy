package ai.diffy.functional;

import ai.diffy.functional.algebra.monoids.functions.SymmetricUnaryOperator;
import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.topology.InvocationLogger;

import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public class Test {
    public static void main(String [] args){
        Endpoint<String,String> repeat = Endpoint.from("repeat", () -> (str)->(str+" "+str));
        System.out.println(repeat.apply("lower"));
        Endpoint<String, String> repeatlower = repeat
                .withMiddleware(apply -> (str) -> str.toLowerCase())
                .withName("repeatlower");
        System.out.println(repeatlower.apply("UPPER"));
        Endpoint<String, String> composed = Endpoint.from("composed", repeat, repeatlower, (_repeat, _repeatlower) -> _repeat.andThen(_repeatlower));
        System.out.println(composed.apply("UppeR"));
        System.out.println(
            composed.deepTransform(InvocationLogger::mapper)
            .apply("bubbles are BIG"));

        ThreadLocal<Stack<Span>> stack = ThreadLocal.withInitial(Stack::new);

        // Environment setup
        AtomicInteger requestId = new AtomicInteger(1000);
        ConcurrentLinkedQueue<Span> traces = new ConcurrentLinkedQueue<>();
        AtomicInteger globalEventCount = new AtomicInteger(0);
        // Environment ready
        Stream.of("Macho", "Man", "Randy", "Savage").parallel().forEach(input -> {
            // Create request context
            AtomicInteger eventCount = new AtomicInteger(0);
            assert stack.get().isEmpty();
            final Span root = new Span();
            stack.get().push(root);
            traces.add(root);
            root.spanId = "trace # " + requestId.getAndAdd(1000);
            root.invocation = new Span.Invocation();
            root.invocation.startSequenceNumber = eventCount.getAndIncrement();
            // Request context ready
            String output = composed.deepTransform((e, d) -> {
                SymmetricUnaryOperator<Object, Object> existingMiddleware = e.getMiddleware();
                SymmetricUnaryOperator<Object, Object> newMiddleware = applier -> (Function)((t) -> {
                    // Create new span
                    Span span = new Span();
                    // Set parent span from stack
                    span.parent = stack.get().peek();
                    // Add the created span to its parent as a child
                    span.parent.children.add(span);
                    span.spanId = "span # "+ span.parent.children.stream().toList().indexOf(span);
                    Span.Invocation invocation = new Span.Invocation();
                    span.invocation = invocation;
                    invocation.endpointName = e.getName();
                    invocation.startSequenceNumber = eventCount.getAndIncrement();
                    System.out.println(span.spanId + " start # " + globalEventCount.getAndIncrement());
                    invocation.input = t;
                    // Add the span to the call stack to make it available as a parent to its children
                    stack.get().push(span);
                    invocation.output = applier.apply(t);
                    //Remove from call stack now that execution is complete
                    // TODO: Think about what happens when ```applier.apply``` is asynchronous.
                    stack.get().pop();
                    invocation.endSequenceNumber = eventCount.getAndIncrement();
                    System.out.println(span.spanId + " end # " + globalEventCount.getAndIncrement());
                    return invocation.output;
                });

                return e.withDownstream(d).withMiddleware(newMiddleware.compose(existingMiddleware));
            }).apply(input);
            System.out.println(input +" -> "+output);
        });

        traces.forEach(x -> print("root", x));
    }
    static void print(String prefix, Span span){
        System.out.println(
                prefix + " " + span.spanId
                        + " invocation # " + span.invocation.startSequenceNumber + " -> " + span.invocation.endSequenceNumber + " : "
                        + span.invocation.endpointName + "(" + span.invocation.input + ") => " + span.invocation.output
        );
        span.children.forEach(x -> Test.print( prefix + " " + span.spanId, x));
    }

    static class Span {
        static class Invocation {
            String endpointName;
            Integer startSequenceNumber;
            Integer endSequenceNumber;
            Object input;
            Object output;
        }
        String spanId;
        Invocation invocation;

        Span parent;
        ConcurrentLinkedQueue<Span> children = new ConcurrentLinkedQueue<>();
    }
}
