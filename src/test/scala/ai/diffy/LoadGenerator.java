package ai.diffy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.LoggerFactory;
import reactor.netty.http.client.HttpClient;

import java.util.stream.IntStream;

public class LoadGenerator {
    public static void main(String[] args) {
        Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        int workload = 1024/128;
        int concurrency = 8;
        int sequence = workload/concurrency;
        long begin = System.currentTimeMillis();
        long cpuTime = IntStream.range(0, concurrency).parallel().mapToLong(i -> {
            HttpClient client = HttpClient.create().port(8880);
            long start = System.currentTimeMillis();
            IntStream.range(0, sequence).forEach(request ->
                    client.headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON))
                            .get()// Specifies that POST method will be used
                            .uri("/json?Mixpanel")   // Specifies the path
                            .responseContent()    // Receives the response body
                            .aggregate()
                            .asString()
                            .block()
            );
            long duration = System.currentTimeMillis() - start;
            System.out.println("loadgen[" + i + "] finished in " + duration + " ms");
            return duration;
        }).reduce(0, (a,b)-> a+b);
        long clockTime = System.currentTimeMillis() - begin;
        System.out.println("Total requests = "+ workload);
        System.out.println("Concurrency level = "+ concurrency);
        System.out.println("Total clock time = "+ clockTime + " ms");
        System.out.println("Total cpu time = "+ cpuTime + " ms");
        System.out.println("Average cpu time per request = " + 1.0*cpuTime/workload + " ms");
        System.out.println("Throughput = " + (1000 * workload) / clockTime + " rps");
    }
}
