package ai.diffy.proxy;

import ai.diffy.functional.endpoints.IndependentEndpoint;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;

public class HttpEndpoint extends IndependentEndpoint<HttpServerRequest, HttpResponse> {
    public HttpEndpoint(String name, HttpClient client) {
        super(name, () -> (HttpServerRequest req) ->
            client
                .headers(headers -> headers.add(req.requestHeaders()))
                .request(req.method())
                .uri(req.uri())
                .send(req.receive().retain())
                .responseSingle(
                    (headers, body) ->
                        body.asString()
                            .map(b -> new HttpResponse(headers.status().toString(), headers.responseHeaders(), b))
                ).block()
        );

    }
    public static HttpEndpoint from(String name, String host, int port) {
        final HttpClient client = HttpClient
                .create().host(host).port(port);
        return new HttpEndpoint(name, client);
    }
}
