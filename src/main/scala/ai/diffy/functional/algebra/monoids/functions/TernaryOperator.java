package ai.diffy.functional.algebra.monoids.functions;

import ai.diffy.functional.functions.TriFunction;

import java.util.function.Function;

@FunctionalInterface
public interface TernaryOperator<
        RequestIn,
        Request1Out, Response1In,
        Request2Out, Response2In,
        Request3Out, Response3In,
        ResponseOut
        > extends TriFunction<
                Function<Request1Out, Response1In>,
                Function<Request2Out, Response2In>,
                Function<Request3Out, Response3In>,
                Function<RequestIn,ResponseOut>
                > {}