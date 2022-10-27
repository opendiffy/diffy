package ai.diffy.functional.algebra.monoids.suppliers;

import ai.diffy.functional.functions.QuadFunction;

import java.util.function.Supplier;

@FunctionalInterface
public interface QuadSupplierOperator<A,B,C,D,Z>
        extends QuadFunction<
                Supplier<A>,
                Supplier<B>,
                Supplier<C>,
                Supplier<D>,
                Supplier<Z>> { }
