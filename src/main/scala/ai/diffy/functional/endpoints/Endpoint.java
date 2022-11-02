package ai.diffy.functional.endpoints;

import ai.diffy.functional.algebra.monoids.functions.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Endpoint<Request, Response> implements Function<Request, Response> {
    protected String name;
    private final Function<Request, Response> applier;
    private volatile SymmetricUnaryOperator<Request, Response> collapsedMiddleware = apply -> apply;

    protected Endpoint(String name, Function<Request, Response> applier){
        this.name = name;
        this.applier = applier;
    }

    @Override
    public Response apply(Request request) {
        /**
         * This is where we pass context to our dependencies
         */
        return collapsedMiddleware.apply(applier).apply(request);
    }

    public <RequestIn, ResponseOut> Endpoint<RequestIn, ResponseOut> map(UnaryOperator<RequestIn, Request, Response,ResponseOut> operator){
        return Endpoint.from(getName(), () -> (requestIn) -> operator.apply(applier).apply(requestIn));
    }
    public abstract List<Endpoint> getDownstream();
    public abstract Endpoint<Request, Response> deepClone();
    public Endpoint<Request, Response> deepTransform(SymmetricUnaryOperator<Request, Response> operator) {
        return deepTransform((e,d)->
            e.withDownstream(d)
            .overrideMiddleware(getMiddleware().andThen(operator))
        );
    }
    public Endpoint<Request, Response> deepTransform(BiFunction<Endpoint, List<Endpoint>, Endpoint> mapper){
        Set<Endpoint> deep = deepDown();
        ConcurrentHashMap<Endpoint, Endpoint> transformed = new ConcurrentHashMap<>(deep.size());

        while (transformed.size() < deep.size()) {
            deep.stream().filter( e ->
                !transformed.containsKey(e) &&
                e.getDownstream().stream()
                .filter(d -> !transformed.containsKey(d)).toList().isEmpty()
            ).forEach(e -> {
                List<Endpoint> transformedDownstream = e.getDownstream().stream().map(d -> transformed.get(d)).toList();
                transformed.put(e, mapper.apply(e, transformedDownstream));
            });
        }
        return transformed.get(this);
    }
    private Set<Endpoint> deepDown(){
        return (Set<Endpoint>) Stream.concat(
            Stream.of(this),
            this.getDownstream().stream()
                .map(Endpoint::deepDown)
                .flatMap(Set::stream)
        ).collect(Collectors.toSet());
    }

    public SymmetricUnaryOperator<Request, Response> getMiddleware(){
        return collapsedMiddleware;
    }
    protected Endpoint<Request, Response> setMiddleware(SymmetricUnaryOperator<Request, Response> operator){
        this.collapsedMiddleware = operator;
        return this;
    }
    public Endpoint<Request, Response> composeMiddleware(SymmetricUnaryOperator<Request, Response> operator){
        Endpoint<Request, Response> result = withDownstream(getDownstream());
        result.setMiddleware(getMiddleware().compose(operator));
        return result;
    }
    public Endpoint<Request, Response> andThenMiddleware(SymmetricUnaryOperator<Request, Response> operator){
        Endpoint<Request, Response> result = withDownstream(getDownstream());
        result.setMiddleware(getMiddleware().andThen(operator));
        return result;
    }
    public Endpoint<Request, Response> overrideMiddleware(SymmetricUnaryOperator<Request, Response> operator){
        Endpoint<Request, Response> result = withDownstream(getDownstream());
        result.setMiddleware(operator);
        return result;
    }
    public String getName() {
        return name;
    }
    protected void setName(String name) {
        this.name = name;
    }
    public Endpoint<Request, Response> withName(String name){
        Endpoint<Request, Response> result = withDownstream(getDownstream());
        result.setName(name);
        return result;
    }
    public abstract Endpoint<Request, Response> withDownstream(List<Endpoint> downstream);

    public static <Req, Rep> IndependentEndpoint<Req, Rep> from(String name, NullOperator<Req, Rep> filter){
        return new IndependentEndpoint<>(name, filter);
    }

    public static <RequestIn,
            RequestOut, ResponseIn,
            ResponseOut> DependentEndpoint from(
            String name,
            Endpoint<RequestOut, ResponseIn> dependency,
            UnaryOperator<RequestIn,
            RequestOut, ResponseIn,
            ResponseOut> unaryOperator){
        return new DependentEndpoint(
                name,
                dependency,
                unaryOperator
        );
    }


    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            ResponseOut> BiDependentEndpoint from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            BinaryOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            ResponseOut> filter){
        return new BiDependentEndpoint(
                name,
                dependency1,
                dependency2,
                filter
        );
    }

    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            ResponseOut> TriDependentEndpoint from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            TernaryOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            ResponseOut> filter){
        return new TriDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                filter
        );
    }

    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            ResponseOut> QuadDependentEndpoint from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            Endpoint<Request4, Response4> dependency4,
            QuadOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            ResponseOut> filter){
        return new QuadDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                dependency4,
                filter
        );
    }
    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            ResponseOut> PentaDependentEndpoint from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            Endpoint<Request4, Response4> dependency4,
            Endpoint<Request5, Response5> dependency5,
            PentaOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            ResponseOut> filter){
        return new PentaDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                dependency4,
                dependency5,
                filter
        );
    }

    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            ResponseOut> HexaDependentEndpoint from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            Endpoint<Request4, Response4> dependency4,
            Endpoint<Request5, Response5> dependency5,
            Endpoint<Request6, Response6> dependency6,
            HexaOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            ResponseOut> filter){
        return new HexaDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                dependency4,
                dependency5,
                dependency6,
                filter
        );
    }

    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            Request7, Response7,
            ResponseOut> SeptaDependentEndpoint                                                                                                                             from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            Endpoint<Request4, Response4> dependency4,
            Endpoint<Request5, Response5> dependency5,
            Endpoint<Request6, Response6> dependency6,
            Endpoint<Request7, Response7> dependency7,
            SeptaOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            Request7, Response7,
            ResponseOut> filter){
        return new SeptaDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                dependency4,
                dependency5,
                dependency6,
                dependency7,
                filter
        );
    }


    public static <RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            Request7, Response7,
            Request8, Response8,
            ResponseOut> OctaDependentEndpoint                                                                                                                             from(
            String name,
            Endpoint<Request1, Response1> dependency1,
            Endpoint<Request2, Response2> dependency2,
            Endpoint<Request3, Response3> dependency3,
            Endpoint<Request4, Response4> dependency4,
            Endpoint<Request5, Response5> dependency5,
            Endpoint<Request6, Response6> dependency6,
            Endpoint<Request7, Response7> dependency7,
            Endpoint<Request8, Response8> dependency8,
            OctaOperator<RequestIn,
            Request1, Response1,
            Request2, Response2,
            Request3, Response3,
            Request4, Response4,
            Request5, Response5,
            Request6, Response6,
            Request7, Response7,
            Request8, Response8,
            ResponseOut> filter){
        return new OctaDependentEndpoint(
                name,
                dependency1,
                dependency2,
                dependency3,
                dependency4,
                dependency5,
                dependency6,
                dependency7,
                dependency8,
                filter
        );
    }
}
