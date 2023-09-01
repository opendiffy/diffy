package ai.diffy.compare;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class DifferenceTest {

    @Test
    public void payloadTest() throws IOException {
        FileSystemResource payload = new FileSystemResource("src/test/resources/payload.json");
        String json = FileCopyUtils.copyToString(new InputStreamReader(payload.getInputStream()));
        log.info(json);
        Difference diff = Difference.apply(json, json);
        diff.flattened().foreach(tuple -> {
            log.info("{} - {}", tuple._1(), tuple._2());
            return null;
        });
    }
}
