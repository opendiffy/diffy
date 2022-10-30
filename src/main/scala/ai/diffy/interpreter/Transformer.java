package ai.diffy.interpreter;

import ai.diffy.functional.algebra.Bijection;
import ai.diffy.functional.algebra.UnsafeFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class Transformer<A> implements UnsafeFunction<A, A> {
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

    private final UnsafeFunction<A, A> applier;

    public Transformer(Class<A> cls, String transformationSource){
        this(cls, Source.create("js", transformationSource));
    }
    public Transformer(Class<A> cls, Source transformationSource){
        Value parser = context.get().eval(parserSource);
        Value stringify = context.get().eval(stringifySource);
        Bijection<String, Value> parseJson = Bijection.of(parser::execute, (obj) -> stringify.execute(obj).asString());
        stringify.execute(parser.execute("{}")); // warmup
        Value transformation = context.get().eval(transformationSource);
        Bijection<A, String> serializeToJson = Bijection.of(mapper::writeValueAsString, str -> mapper.readValue(str, cls));
        this.applier = serializeToJson.andThen(parseJson).wrap(transformation::execute);
    }

    @Override
    public A apply(A a) throws Throwable {
        return applier.apply(a);
    }
}
