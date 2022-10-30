package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.Endpoint;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;

public class ControlFlowLogger {
    private final List<Tuple3<String, Object, Object>> events = new ArrayList<>();

    public <Request, Response> Endpoint<Request, Response> mapper(Endpoint<Request, Response> e, List<Endpoint> d) {
        return e.andThenMiddleware((apply) -> (request) -> {
            Response result = apply.apply(request);
            events.add(Tuples.of(e.getName(), request, result));
            return result;
        }).withDownstream(d);
    }
}
