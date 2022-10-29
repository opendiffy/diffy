package ai.diffy.interpreter;

import ai.diffy.proxy.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.HeadersUtils;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Tranformer<A> implements Function<A, A> {
    private static ObjectMapper mapper = new ObjectMapper();
    private static ThreadLocal<Context> context =
        ThreadLocal.withInitial(() ->
            Context
                .newBuilder("js")
                .option("js.ecmascript-version", "2022")
                .engine(Engine
                    .newBuilder()
                    .option("engine.WarnInterpreterOnly","false")
                    .build())
                .build()
        );

    private static Source parserSource = Source.create("js", "(x)=>(JSON.parse(x))");
    private static Source stringifySource = Source.create("js", "(x)=>(JSON.stringify(x))");

    private final Value parser;
    private final Value stringify;
    private final Value transformation;
    private final Class<A> cls;
    public Tranformer(Class<A> cls, String transformationSource){
        this(cls, Source.create("js", transformationSource));
    }
    private Tranformer(Class<A> cls, Source transformationSource){
        this.parser = context.get().eval(parserSource);
        this.stringify = context.get().eval(stringifySource);
        this.stringify.execute(this.parser.execute("{}")); // warmup
        this.transformation = context.get().eval(transformationSource);
        this.cls = cls;
    }

    @Override
    public A apply(A a) {
        try {
            return mapper.readValue(
                    stringify.execute(
                            transformation.execute(
                                    parser.execute(
                                            mapper.writeValueAsString(a)
                                    )
                            )
                    ).asString(),
                    cls
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String [] args) {
        try {
            Source tx = Source.newBuilder("js", new File("src/main/scala/ai/diffy/interpreter/transform.js")).build();
            Tranformer<HttpRequest> reqTx = new Tranformer<>(HttpRequest.class, tx);
            Map<String, String> params = new HashMap<>();
            params.put("hello", "world");
            HttpHeaders headers = EmptyHttpHeaders.INSTANCE;
            HttpRequest request = new HttpRequest("POST", "/json?Mixpanel", "/json",params,headers,"oh yeah!");
            HttpRequest transformed = reqTx.apply(request);
            System.out.println("All done!");
            System.out.println("\n\nHere's the original:\n"+request);
            System.out.println("\n\nHere's the transformed:\n"+transformed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
