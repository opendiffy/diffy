package ai.diffy.interpreter;

import ai.diffy.functional.algebra.Bijection;
import ai.diffy.functional.algebra.UnsafeFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class Lambda<Request, Response> implements UnsafeFunction<Request, Response> {
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

    private final UnsafeFunction<Request, Response> applier;

    public Lambda(Class<Response> clsResponse, String lambda){
        this(clsResponse, Source.create("js", lambda));
    }
    public Lambda(Class<Response> clsResponse,  Source lambda){
        synchronized (context.get()) {
            Value parser = context.get().eval(parserSource);
            Value stringify = context.get().eval(stringifySource);
            Bijection<String, Value> parseJson = Bijection.of(parser::execute, (obj) -> stringify.execute(obj).asString());
            stringify.execute(parser.execute("{}")); // warmup
            Value transformation = context.get().eval(lambda);
            UnsafeFunction<Request, String> stringifyRequest = mapper::writeValueAsString;
            UnsafeFunction<String, Response> parseResponse = str -> mapper.readValue(str, clsResponse);
            this.applier = stringifyRequest.andThen(parseJson.wrap(transformation::execute)).andThen(parseResponse);
        }
    }

    @Override
    public Response apply(Request request) throws Throwable {
        return applier.apply(request);
    }
}
