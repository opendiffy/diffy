package ai.diffy.functional.algebra.monoids.suppliers;

import ai.diffy.functional.functions.TriFunction;

import java.util.function.Supplier;

@FunctionalInterface
public interface TriSupplierOperator<A,B,C, Z> extends TriFunction<Supplier<A>, Supplier<B>, Supplier<C>, Supplier<Z>> { }
