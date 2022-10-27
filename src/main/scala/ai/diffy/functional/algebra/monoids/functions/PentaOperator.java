package ai.diffy.functional.algebra.monoids.functions;

import ai.diffy.functional.functions.PentaFunction;

import java.util.function.Function;

/**
 *
 * @param <RequestIn> The initial incoming request
 * The following endpoints may be called in any order any number of times.
 * NamedEndpoint<Request1Out, Response1In>,
 * NamedEndpoint<Request2Out, Response2In>,
 * NamedEndpoint<Request3Out, Response3In>,
 * NamedEndpoint<Request4Out, Response4In>,
 * NamedEndpoint<Request5Out, Response5In>,
 * The numbers 1,2,3,... only serve the purpose of enumeration and do not imply
 * any ordering whatsoever.
 * @param <ResponseOut> The final out going response
 */

@FunctionalInterface
public interface PentaOperator<
        RequestIn,
        Request1Out, Response1In,
        Request2Out, Response2In,
        Request3Out, Response3In,
        Request4Out, Response4In,
        Request5Out, Response5In,
        ResponseOut
        > extends PentaFunction<
                Function<Request1Out, Response1In>,
                Function<Request2Out, Response2In>,
                Function<Request3Out, Response3In>,
                Function<Request4Out, Response4In>,
                Function<Request5Out, Response5In>,
                Function<RequestIn,ResponseOut>
                > {}
