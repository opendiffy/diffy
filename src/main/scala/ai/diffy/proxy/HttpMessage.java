package ai.diffy.proxy;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HttpMessage {
    HttpHeaders headers;
    String body;

    public HttpMessage(HttpHeaders headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    public Map<String, String> getHeaders(){
        Map<String, List<String>> grouped = new TreeMap<>();
        this.headers.entries().forEach(entry -> {
            grouped.putIfAbsent(entry.getKey(), new ArrayList<>());
            grouped.get(entry.getKey()).add(entry.getValue());
        });
        Map<String, String> map = new TreeMap<>();
        grouped.entrySet().forEach(entry -> {
            map.put(entry.getKey(), entry.getValue().stream().sorted().collect(Collectors.joining(", ")));
        });
        return map;
    }

    public String getBody(){
        return body;
    }

    @Override
    public String toString() {
        String headers = this.headers.entries().stream().map(entry -> entry.getKey() + " : " + entry.getValue()).reduce((e1, e2) -> e1 + "\n" + e2).orElse("");
        return "\n" + headers + "\n" + body;
    }
}
