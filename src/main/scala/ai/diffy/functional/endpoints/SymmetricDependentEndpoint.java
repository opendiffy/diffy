package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.UnaryOperator;

public class SymmetricDependentEndpoint<Request, Response>
        extends DependentEndpoint<Request, Request, Response, Response> {
    public SymmetricDependentEndpoint(
            String name,
            Endpoint<Request, Response> dependency,
            UnaryOperator<Request, Request, Response, Response> unaryOperator) {
        super(name, dependency, unaryOperator);
    }
}