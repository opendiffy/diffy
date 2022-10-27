package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface SeptaFunction<A,B,C,D,E,F,G, Z> {

    Z apply(A a, B b, C c, D d, E e, F f, G g);

    default <Y> SeptaFunction<A,B,C,D,E,F,G,Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d, E e, F f, G g) -> after.apply(apply(a,b,c,d,e,f,g));
    }
}
