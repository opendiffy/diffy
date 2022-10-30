package ai.diffy.functional.algebra;

import java.util.Objects;

public class Bijection<T, R> {
    private final UnsafeFunction<T, R> applier;
    private final UnsafeFunction<R, T> unapplier;
    private Bijection(UnsafeFunction<T, R> applier, UnsafeFunction<R,T> unapplier) {
        Objects.requireNonNull(applier);
        Objects.requireNonNull(unapplier);

        this.applier = applier;
        this.unapplier = unapplier;
    }
    public static <A, B> Bijection<A, B> of(UnsafeFunction<A, B> applier, UnsafeFunction<B, A> unapplier) {
        return new Bijection<>(applier, unapplier);
    }
    public T unapply(R r) throws Throwable {
        return unapplier.apply(r);
    }

    public R apply(T t) throws Throwable {
        return applier.apply(t);
    }

    public <V> Bijection<V, R> compose(Bijection<V,T> before) {
        Objects.requireNonNull(before);
        return Bijection.of(applier.compose(before.applier), unapplier.andThen(before.unapplier));
    }

    public <V> Bijection<T, V> andThen(Bijection<R, V> after) {
        Objects.requireNonNull(after);
        return Bijection.of(applier.andThen(after.applier), unapplier.compose(after.unapplier));

    }

    public UnsafeFunction<T, T> wrap(UnsafeFunction<R, R> wrapped) {
        return applier.andThen(wrapped).andThen(unapplier);
    }

    public static <T> Bijection<T, T> identity() {
        return Bijection.of(t -> t, t -> t);
    }
}
