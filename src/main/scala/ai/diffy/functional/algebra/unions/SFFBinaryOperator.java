package ai.diffy.functional.algebra.unions;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface SFFBinaryOperator<A,B,C, Z1,Z2> extends BiFunction<Supplier<A>, Function<B, C>, Function<Z1,Z2>> {}
