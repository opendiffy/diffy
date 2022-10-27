package ai.diffy.functional.functions;

@FunctionalInterface
public interface Proceeder<T> {
	T get() throws Throwable;
}