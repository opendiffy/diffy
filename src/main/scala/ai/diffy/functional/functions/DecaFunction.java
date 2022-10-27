package ai.diffy.functional.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface DecaFunction<A,B,C,D,E,F,G,H,I,J, Z> {

    Z apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);

    default <Y> DecaFunction<A,B,C,D,E,F,G,H,I,J, Y> andThen(Function<? super Z, ? extends Y> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d, E e, F f, G g, H h, I i, J j) -> after.apply(apply(a,b,c,d,e,f,g,h,i,j));
    }
}
