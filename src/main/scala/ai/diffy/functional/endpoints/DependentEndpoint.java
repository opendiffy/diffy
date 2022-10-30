package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.UnaryOperator;

import java.util.List;

public class DependentEndpoint<
        RequestIn,
        RequestOut, ResponseIn,
        ResponseOut> extends Endpoint<RequestIn, ResponseOut> {
    private final Endpoint<RequestOut, ResponseIn> dependency;
    private final UnaryOperator<RequestIn,
                        RequestOut, ResponseIn,
                        ResponseOut> unaryOperator;
    protected DependentEndpoint(
        String name,
        Endpoint<RequestOut, ResponseIn> dependency,
        UnaryOperator<RequestIn,
                                    RequestOut, ResponseIn,
                                    ResponseOut> unaryOperator) {
        super(name, unaryOperator.apply(dependency));
        this.dependency = dependency;
        this.unaryOperator = unaryOperator;
    }

    @Override
    public List<Endpoint> getDownstream() {
        return List.of(dependency);
    }

    @Override
    public Endpoint<RequestIn, ResponseOut> deepClone() {
        return new DependentEndpoint<>(
                this.name,
                dependency.deepClone(),
                unaryOperator).setMiddleware(this.getMiddleware());
    }

    @Override
    public Endpoint<RequestIn, ResponseOut> withDownstream(List<Endpoint> downstream) {
        assert downstream.size() == 1;
        assert this.dependency.getClass().isAssignableFrom(downstream.get(0).getClass());
        return new DependentEndpoint<>(this.name, downstream.get(0), this.unaryOperator)
                .setMiddleware(this.getMiddleware());
    }
}