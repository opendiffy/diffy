package ai.diffy.proxy;

import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String uri;
    private final String path;
    private final Map<String, String> params;
    private final HttpMessage message;

    public HttpRequest(String method, String uri, String path, Map<String, String> params, HttpMessage message) {
        this.method = method;
        this.uri = uri;
        this.path = path;
        this.params = params;
        this.message = message;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public HttpMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "path = "+ path+"\n"+"params =\n"+ params+"\n"+"message =\n"+ message+"\n";
    }
}
