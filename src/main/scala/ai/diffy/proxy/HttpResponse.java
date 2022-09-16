package ai.diffy.proxy;

import java.util.Map;

public class HttpResponse {
    private final String status;
    private final HttpMessage message;

    public HttpResponse(String status, HttpMessage message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public HttpMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "\nstatus = "+ status+"\n"+"message =\n"+ message+"\n";
    }
}
