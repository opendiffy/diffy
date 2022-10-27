package ai.diffy.functional.algebra.monoids.suppliers;

import java.util.function.Supplier;

@FunctionalInterface
public interface NullSupplierOperator<A> extends Supplier<Supplier<A>> { }
