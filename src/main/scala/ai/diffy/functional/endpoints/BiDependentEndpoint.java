package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.BinaryOperator;

import java.util.List;

public class BiDependentEndpoint<
        RequestIn,
        Request1, Response1,
        Request2, Response2,
        ResponseOut> extends Endpoint<RequestIn, ResponseOut> {

    private final Endpoint<Request1, Response1> dependency1;
    private final Endpoint<Request2, Response2> dependency2;

    private final BinaryOperator<RequestIn,
                    Request1, Response1,
                    Request2, Response2,
                    ResponseOut> filter;
    protected BiDependentEndpoint(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            BinaryOperator<RequestIn,
                                    Request1, Response1,
                                    Request2, Response2,
                                    ResponseOut> filter) {
        super(name, filter.apply(dependency1, dependency2));
        this.dependency1 = dependency1;
        this.dependency2 = dependency2;
        this.filter = filter;
    }

    @Override
    public List<Endpoint> getDownstream() {
        return List.of(dependency1, dependency2);
    }

    @Override
    public Endpoint<RequestIn, ResponseOut> deepClone() {
        return new BiDependentEndpoint<>(
                this.name,
                dependency1.deepClone(),
                dependency2.deepClone(),
                filter).setMiddleware(this.getMiddleware());
    }

    @Override
    public Endpoint<RequestIn, ResponseOut> withDownstream(List<Endpoint> downstream) {
        assert downstream.size() == 2;
        assert this.dependency1.getClass().isAssignableFrom(downstream.get(0).getClass());
        assert this.dependency2.getClass().isAssignableFrom(downstream.get(1).getClass());
        return new BiDependentEndpoint<>(
                this.name,
                downstream.get(0),
                downstream.get(1),
                filter).setMiddleware(this.getMiddleware());
    }
}