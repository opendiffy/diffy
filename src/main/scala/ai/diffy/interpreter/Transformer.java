package ai.diffy.interpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.function.Function;

public class Transformer<A> implements Function<A, A> {
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
    public Transformer(Class<A> cls, String transformationSource){
        this(cls, Source.create("js", transformationSource));
    }
    public Transformer(Class<A> cls, Source transformationSource){
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
}
