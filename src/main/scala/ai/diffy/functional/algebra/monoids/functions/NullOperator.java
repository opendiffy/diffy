package ai.diffy.functional.algebra.monoids.functions;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface NullOperator<
        RequestIn,
        ResponseOut
        > extends Supplier<
        Function<RequestIn, ResponseOut>  // Inbound Ingress call
        > {}
