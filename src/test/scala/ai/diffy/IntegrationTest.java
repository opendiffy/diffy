package ai.diffy;

import ai.diffy.interpreter.http.HttpLambdaServer;
import ai.diffy.proxy.ReactorHttpDifferenceProxy;
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
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    RestTemplate restTemplate = new RestTemplate();
    @Autowired
    Settings settings;

    @Autowired
    ReactorHttpDifferenceProxy proxy;
    private Integer extractPort(Downstream downstream) {
        HostPort hostPort = (HostPort) downstream;
        return hostPort.port();
    }
    HttpLambdaServer primary, secondary, candidate;
    @BeforeAll
    public void setup() throws IOException {
        primary = new HttpLambdaServer(extractPort(settings.primary()), new File("src/test/resources/lambda.js"));
        secondary = new HttpLambdaServer(extractPort(settings.secondary()), new File("src/test/resources/lambda.js"));
        candidate = new HttpLambdaServer(extractPort(settings.candidate()), new File("src/test/resources/lambda.js"));
    }

    @AfterAll
    public void shutdown() {
        primary.shutdown();
        secondary.shutdown();
        candidate.shutdown();
    }

    @Test
    public void warmup() throws Exception {
        String proxyUrl = "http://localhost:"+settings.servicePort()+"/base";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        FileSystemResource payload = new FileSystemResource("src/test/resources/payload.json");
        String json = FileCopyUtils.copyToString(new InputStreamReader(payload.getInputStream()));
        String response = restTemplate.postForObject(proxyUrl, new HttpEntity<String>(json, headers), String.class);
        assertEquals(json, response);
    }
}
