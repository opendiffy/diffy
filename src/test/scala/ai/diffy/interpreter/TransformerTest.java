package ai.diffy.interpreter;

import ai.diffy.proxy.HttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TransformerTest {
    public static void main(String [] args) {
        try {
            Source tx = Source.newBuilder("js", new File("src/test/scala/ai/diffy/interpreter/transform.js")).build();
            Transformer<HttpRequest> reqTx = new Transformer<>(HttpRequest.class, tx);
            Map<String, String> params = new HashMap<>();
            params.put("hello", "world");
            HttpHeaders headers = EmptyHttpHeaders.INSTANCE;
            HttpRequest request = new HttpRequest("POST", "/json?Mixpanel", "/json",params,headers,"oh yeah!");
            HttpRequest transformed = reqTx.apply(request);
            System.out.println("All done!");
            System.out.println("\n\nHere's the original:\n"+request);
            System.out.println("\n\nHere's the transformed:\n"+transformed);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
