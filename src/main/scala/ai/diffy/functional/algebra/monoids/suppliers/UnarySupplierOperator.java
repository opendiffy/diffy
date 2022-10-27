package ai.diffy.functional.algebra.monoids.suppliers;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface UnarySupplierOperator<A,B> extends Function<Supplier<A>, Supplier<B>> { }
