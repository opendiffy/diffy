package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<A,B,C,Z> {

    Z apply(A a, B b, C c);

    default <Y> TriFunction<A,B,C,Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a,b,c));
    }
}
