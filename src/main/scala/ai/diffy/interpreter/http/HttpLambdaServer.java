package ai.diffy.interpreter.http;

import ai.diffy.interpreter.Lambda;
import ai.diffy.proxy.HttpEndpoint;
import ai.diffy.proxy.HttpMessage;
import ai.diffy.proxy.HttpRequest;
import ai.diffy.proxy.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.graalvm.polyglot.Source;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class HttpLambdaServer {
    private final DisposableServer server;
    private final Function<HttpRequest, HttpResponse> lambda;
    public HttpLambdaServer(int port, String httpLambda) {
        this(port, Source.create("js", httpLambda));
    }
    public HttpLambdaServer(int port, File httpLambda) throws IOException {
        this(port, Source.newBuilder("js", httpLambda).build());
    }
    public HttpLambdaServer(int port, Source httpLambda) {
        lambda = new Lambda<HttpRequest, HttpResponse>(HttpResponse.class, httpLambda).suppressThrowable();
        server = HttpServer.create()
                .port(port)
                .httpRequestDecoder(httpRequestDecoderSpec ->
                        httpRequestDecoderSpec
                                .maxChunkSize(32*1024*1024)
                                .maxHeaderSize(32*1024*1024))
                .handle((req, res) ->
                    Mono.fromFuture(
                        HttpEndpoint.RequestBuffer
                            .apply(req)
                            .thenApply(lambda)
                    ).flatMap(r ->
                        res
                            .status(HttpResponseStatus.parseLine(r.getStatus()))
                            .headers(HttpMessage.toHttpHeaders(r.getHeaders()))
                            .sendString(Mono.justOrEmpty(r.getBody()))
                            .then()
                    )
                ).bindNow();
    }

    public void shutdown() {
        server.disposeNow();
    }
    public static void main(String[] args) throws Exception {
        HttpLambdaServer primary = new HttpLambdaServer(Integer.parseInt(args[0]), new File("src/main/scala/ai/diffy/interpreter/http/master.js"));
        HttpLambdaServer secondary = new HttpLambdaServer(Integer.parseInt(args[1]), new File("src/main/scala/ai/diffy/interpreter/http/master.js"));
        HttpLambdaServer candidate = new HttpLambdaServer(Integer.parseInt(args[2]), new File("src/main/scala/ai/diffy/interpreter/http/candidate.js"));

        primary.server.onDispose().block();
        secondary.server.onDispose().block();
        candidate.server.onDispose().block();
    }
}
