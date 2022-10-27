package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.NullOperator;

import java.util.List;

public class IndependentEndpoint<Request, Response> extends Endpoint<Request, Response>{
    private final NullOperator<Request, Response> filter;
    public IndependentEndpoint(String name, NullOperator<Request, Response> filter) {
        super(name, filter.get());
        this.filter = filter;
    }

    @Override
    public List<Endpoint> getDownstream() {
        return List.of();
    }

    @Override
    public Endpoint<Request, Response> deepClone() {
        return new IndependentEndpoint<>(this.name, this.filter).setMiddleware(this.getMiddleware());
    }

    @Override
    public Endpoint<Request, Response> withDownstream(List<Endpoint> downstream) {
        assert downstream.isEmpty();
        return deepClone().setMiddleware(this.getMiddleware());
    }
}
