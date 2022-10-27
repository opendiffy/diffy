package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface OctaFunction<A,B,C,D,E,F,G,H, Z> {

    Z apply(A a, B b, C c, D d, E e, F f, G g, H h);

    default <Y> OctaFunction<A,B,C,D,E,F,G,H, Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d, E e, F f, G g, H h) -> after.apply(apply(a,b,c,d,e,f,g,h));
    }
}
