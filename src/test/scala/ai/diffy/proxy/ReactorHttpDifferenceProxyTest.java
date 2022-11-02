package ai.diffy.proxy;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public class ReactorHttpDifferenceProxyTest {
    public static void main(String[] args) {
        Mono.fromFuture(CompletableFuture.failedFuture(new RuntimeException()))
                .doOnError(t -> System.out.println("error"))
                .block()
        ;
    }
}
