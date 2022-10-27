package ai.diffy.functional.topology;

import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.endpoints.SymmetricDependentEndpoint;
import io.opentelemetry.api.trace.Span;

public class SpanWrapper<Request, Response> extends SymmetricDependentEndpoint<Request, Response> {
    public SpanWrapper(Endpoint<Request, Response> dependency, TriConsumer<Request, Response, Span> spanLogger) {
        super(dependency.getName()+".span", dependency, applyDependency -> (Request request) -> {
            final Span span = Span.current();
            final Response result = applyDependency.apply(request);
            spanLogger.accept(request, result, span);
            return result;
        });
    }
}
