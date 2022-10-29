package ai.diffy.functional.algebra.monoids.functions;

import java.util.Objects;

@FunctionalInterface
public interface SymmetricUnaryOperator<Request, Response> extends UnaryOperator<Request, Request, Response, Response> {

    default SymmetricUnaryOperator<Request, Response> compose(SymmetricUnaryOperator<Request, Response> before) {
        Objects.requireNonNull(before);
        return (applier) -> apply(before.apply(applier));
    }
    default SymmetricUnaryOperator<Request, Response> andThen(SymmetricUnaryOperator<Request, Response> after) {
        Objects.requireNonNull(after);
        return (applier) -> after.apply(apply(applier));
    }
    static <T> SymmetricUnaryOperator<T, T> identity() {
        return t -> t;
    }
}
