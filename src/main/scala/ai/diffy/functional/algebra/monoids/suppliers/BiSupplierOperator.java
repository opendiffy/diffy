package ai.diffy.functional.algebra.monoids.suppliers;

import java.util.function.BiFunction;
import java.util.function.Supplier;

@FunctionalInterface
public interface BiSupplierOperator<A,B,C> extends BiFunction<Supplier<A>, Supplier<B>, Supplier<C>> { }
