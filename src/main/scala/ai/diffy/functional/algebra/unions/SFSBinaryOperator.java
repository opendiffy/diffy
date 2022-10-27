package ai.diffy.functional.algebra.unions;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface SFSBinaryOperator<A,B,C, Z> extends BiFunction<Supplier<A>, Function<B, C>, Supplier<Z>> { }
