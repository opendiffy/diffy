package ai.diffy.functional.topology;

public interface TriConsumer<Request, Response, Span> {
    void accept(Request request, Response response, Span span);
}
