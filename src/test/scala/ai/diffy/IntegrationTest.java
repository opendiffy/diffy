package ai.diffy;

import ai.diffy.interpreter.http.HttpLambdaServer;
import ai.diffy.proxy.ReactorHttpDifferenceProxy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    RestTemplate restTemplate = new RestTemplate();
    @Autowired
    ai.diffy.Settings settings;

    @Autowired
    ReactorHttpDifferenceProxy proxy;
    private Integer extractPort(Downstream downstream) {
        HostPort hostPort = (HostPort) downstream;
        return hostPort.port();
    }
    HttpLambdaServer primary, secondary, candidate;
    String proxyUrl;
    @BeforeAll
    public void setup() throws IOException {
        primary = new HttpLambdaServer(extractPort(settings.primary()), new File("src/test/resources/echo.js"));
        secondary = new HttpLambdaServer(extractPort(settings.secondary()), new File("src/test/resources/echo.js"));
        candidate = new HttpLambdaServer(extractPort(settings.candidate()), new File("src/test/resources/echo.js"));
        proxyUrl = "http://localhost:"+settings.servicePort()+"/base";
    }

    @AfterAll
    public void shutdown() {
        primary.shutdown();
        secondary.shutdown();
        candidate.shutdown();
    }

    @Test
    public void warmup() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        FileSystemResource payload = new FileSystemResource("src/test/resources/payload.json");
        String json = FileCopyUtils.copyToString(new InputStreamReader(payload.getInputStream()));
        String response = restTemplate.postForObject(proxyUrl, new HttpEntity<>(json, headers), String.class);
        assertEquals(json, response);
    }

    @Test
    public void largeRequestBody() {
        int largeSize = 16*1024*1024; // 16 MB
        String json = "{\"a\":\""+new String(new char[largeSize])+"\"}";
        log.info("Testing request body of {} MB", json.getBytes().length/1024/1024);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String response = restTemplate.postForObject(proxyUrl, new HttpEntity<>(json, headers), String.class);
        assertEquals(json, response);
    }

    @Test
    public void largeRequestHeaders() {
        int largeSize = 16*1024*1024; // 16 MB
        String json = "{\"a\":\"\"}";
        String header = new String(new char[largeSize]).replaceAll(".", "0");
        log.info("Testing request header of {} MB", header.getBytes().length/1024/1024);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("a", header);

        ResponseEntity<String> response = restTemplate.postForEntity(proxyUrl, new HttpEntity<>(json, headers), String.class);
        assertEquals(json, response.getBody());
        assertEquals(header, response.getHeaders().getFirst("a"));
    }
}
