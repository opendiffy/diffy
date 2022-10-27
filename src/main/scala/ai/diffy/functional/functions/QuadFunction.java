package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface QuadFunction<A,B,C,D,Z> {

    Z apply(A a, B b, C c, D d);

    default <Y> QuadFunction<A,B,C,D,Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d) -> after.apply(apply(a,b,c,d));
    }
}
