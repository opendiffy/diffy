package ai.diffy.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Future {
    public static <T> void assign(CompletableFuture<T> original, CompletableFuture<T> incomplete) {
        original.whenComplete((response, error) -> {
            if(error != null) {
                incomplete.completeExceptionally(error);
            } else {
                incomplete.complete(response);
            }
        });
    }

    public static <A, B> CompletableFuture<B> getAfter(CompletableFuture<A> blocker, Supplier<CompletableFuture<B>> main){
        return blocker
                .thenCompose((success) -> main.get())
                .exceptionallyCompose((error) -> main.get());
    }
}
