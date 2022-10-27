package ai.diffy.functional.algebra.monoids.functions;

import ai.diffy.functional.functions.QuadFunction;

import java.util.function.Function;

/**
 *
 * @param <RequestIn> The initial incoming request
 * The following services may be called in any order any number of times.
 *      @param <Request1Out> @param <Response1In>
 *      @param <Request2Out> @param <Response2In>
 *      @param <Request3Out> @param <Response3In>
 *      @param <Request4Out> @param <Response4In>
 * The numbers 1,2,3,4 only serve the purpose of enumeration and do not imply
 * any ordering whatsoever.
 * @param <ResponseOut> The final out going response
 */

@FunctionalInterface
public interface QuadOperator<
        RequestIn,
        Request1Out, Response1In,
        Request2Out, Response2In,
        Request3Out, Response3In,
        Request4Out, Response4In,
        ResponseOut
        > extends QuadFunction<
                Function<Request1Out, Response1In>,
                Function<Request2Out, Response2In>,
                Function<Request3Out, Response3In>,
                Function<Request4Out, Response4In>,
                Function<RequestIn,ResponseOut>
                > {}
