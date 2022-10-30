package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.TernaryOperator;

import java.util.List;

public class TriDependentEndpoint<
        RequestIn,
        Request1, Response1,
        Request2, Response2,
        Request3, Response3,
        ResponseOut> extends Endpoint<RequestIn, ResponseOut> {

    private final Endpoint<Request1, Response1> dependency1;
    private final Endpoint<Request2, Response2> dependency2;
    private final Endpoint<Request3, Response3> dependency3;

    private final TernaryOperator<RequestIn,
                    Request1, Response1,
                    Request2, Response2,
                    Request3, Response3,
                    ResponseOut> filter;
    protected TriDependentEndpoint(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            TernaryOperator<RequestIn,
                                    Request1, Response1,
                                    Request2, Response2,
                                    Request3, Response3,
                                    ResponseOut> filter) {
        super(name, filter.apply(dependency1, dependency2, dependency3));
        this.dependency1 = dependency1;
        this.dependency2 = dependency2;
        this.dependency3 = dependency3;
        this.filter = filter;
    }

    @Override
    public List<Endpoint> getDownstream() {
        return List.of(dependency1, dependency2, dependency3);
    }

    @Override
    public Endpoint<RequestIn, ResponseOut> deepClone() {
        return new TriDependentEndpoint<>(
                this.name,
                dependency1.deepClone(),
                dependency2.deepClone(),
                dependency3.deepClone(),
                filter).setMiddleware(this.getMiddleware());
    }
    @Override
    public Endpoint<RequestIn, ResponseOut> withDownstream(List<Endpoint> downstream) {
        assert downstream.size() == 3;
        assert this.dependency1.getClass().isAssignableFrom(downstream.get(0).getClass());
        assert this.dependency2.getClass().isAssignableFrom(downstream.get(1).getClass());
        assert this.dependency3.getClass().isAssignableFrom(downstream.get(2).getClass());
        return new TriDependentEndpoint<>(
                this.name,
                downstream.get(0),
                downstream.get(1),
                downstream.get(2),
                filter).setMiddleware(this.getMiddleware());
    }
}