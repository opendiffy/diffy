package ai.diffy.functional.algebra.monoids.functions;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface BinaryOperator<
        RequestIn,
        Request1Out, Response1In,
        Request2Out, Response2In,
        ResponseOut
        > extends BiFunction<
        Function<Request1Out, Response1In>,
        Function<Request2Out, Response2In>,
        Function<RequestIn,ResponseOut>
        > {}