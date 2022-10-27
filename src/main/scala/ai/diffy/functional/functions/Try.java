package ai.diffy.functional.functions;

import java.util.function.Function;

public class Try<T> {
    private T normal;
    private Throwable throwable;

    public static <A> Try<A> of(Proceeder<A> proceeder){
        return new Try<>(proceeder);
    }
    public Boolean isNormal(){
        return this.throwable == null;
    }

    public T get() throws Throwable {
        if(isNormal()){
            return this.normal;
        }
        throw this.throwable;
    }

    public Try(Proceeder<T> proceeder) {
        try {
            this.normal = proceeder.get();
        } catch (RuntimeException throwable){
            this.throwable = throwable.getCause();
        } catch (Throwable throwable){
            this.throwable = throwable;
        }
    }

    public T getNormal() {
        return normal;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public <U> Try<U> map(Function<T, U> mapper) {
        return Try.of(() -> mapper.apply(this.get()));
    }

    @Override
    public String toString() {
        return String.format("try = {normal = %s, throwable = %s}", normal, throwable);
    }
}
