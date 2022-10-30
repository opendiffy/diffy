package ai.diffy.functional.algebra;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface UnsafeFunction<T, R> {
    R apply(T t) throws Throwable;

    default <V> UnsafeFunction<V, R> compose(UnsafeFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> UnsafeFunction<T, V> andThen(UnsafeFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    default Function<T,R> suppressThrowable(){
        return (request) -> {
            try {
                return this.apply(request);
            } catch (Throwable e) {
                throw new RuntimeException(e.getCause());
            }
        };
    }
    static <A> UnsafeFunction<A, A> identity() {
        return t -> t;
    }
}
