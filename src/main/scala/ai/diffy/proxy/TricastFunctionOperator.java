package ai.diffy.proxy;

import ai.diffy.functional.algebra.monoids.functions.TernaryOperator;

public interface TricastFunctionOperator<Request, Response> extends TernaryOperator<
        Request,
        Request, Response,
        Request, Response,
        Request, Response,
        Response[]> {
}
