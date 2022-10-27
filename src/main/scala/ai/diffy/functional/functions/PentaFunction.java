package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface PentaFunction<A,B,C,D,E, Z> {

    Z apply(A a, B b, C c, D d, E e);

    default <Y> PentaFunction<A,B,C,D,E,Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d, E e) -> after.apply(apply(a,b,c,d,e));
    }
}
