package ai.diffy.examples.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.function.Function;

public class ExampleHttp404 {
    public static void main(String[] args) throws Exception {
        int primary = Integer.parseInt(args[0]);
        int secondary = Integer.parseInt(args[1]);
        int candidate = Integer.parseInt(args[2]);

        ExampleResponseTemplate pr =
                new ExampleResponseTemplate(
                        200,
                        "application/json",
                        x -> x.toLowerCase()
                );
        ExampleResponseTemplate sr =
                new ExampleResponseTemplate(
                        200,
                        "application/json",
                        x -> x.toLowerCase()
                );
        ExampleResponseTemplate cr =
                new ExampleResponseTemplate(
                        404,
                        "htm/text",
                        x -> x.toLowerCase()
                );
        Thread p = new Thread(() -> bind(primary, pr));
        Thread s = new Thread(() -> bind(secondary, sr));
        Thread c = new Thread(() -> bind(candidate, cr));
        p.start();
        s.start();
        c.start();
        while(true){
            Thread.sleep(10);
        }
    }

    public static void bind(int port, ExampleResponseTemplate responseTemplate) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(
                    "/json",
                    new Handler404(
                            responseTemplate.getStatusCode(),
                            "{\"name\":\"%s\", \"timestamp\":\"%s\"}",
                            responseTemplate.getContentType(),
                            responseTemplate.getLambda()));
            server.createContext(
                    "/html",
                    new Handler404(
                            responseTemplate.getStatusCode(),
                            "<body><name>%s</name><timestamp>%s</timestamp></body>",
                            "text/html",
                            responseTemplate.getLambda()));
            server.setExecutor(null);
            server.start();
        } catch (Exception exception) {
            System.err.println("!!!failed to start!!!");
        }
    }
}

class Handler404 implements HttpHandler {
    private int statusCode;
    private String template;
    private String contentType;
    private Function<String, String> lambda;
    public Handler404(int statusCode, String template, String contentType, Function<String, String> lambda) {
        super();
        this.statusCode = statusCode;
        this.template = template;
        this.contentType = contentType;
        this.lambda = lambda;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        String name  = lambda.apply(t.getRequestURI().getQuery());
        String response = String.format(template, name, System.currentTimeMillis());
        t.getResponseHeaders().add("Content-Type", contentType);
        t.sendResponseHeaders(statusCode, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

class ExampleResponseTemplate {
    private int statusCode;
    private String contentType;
    private Function<String, String> lambda;

    public ExampleResponseTemplate(int statusCode, String contentType, Function<String, String> lambda) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.lambda = lambda;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }


    public Function<String, String> getLambda() {
        return lambda;
    }
}