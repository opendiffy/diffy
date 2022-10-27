package ai.diffy.functional.algebra.monoids.functions;

import java.util.function.Function;

@FunctionalInterface
public interface UnaryOperator<
        RequestIn,
        RequestOut, ResponseIn,
        ResponseOut
        > extends Function<
        Function<RequestOut, ResponseIn>, // Downstream Egress call
        Function<RequestIn, ResponseOut>  // Inbound Ingress call
        > {}
