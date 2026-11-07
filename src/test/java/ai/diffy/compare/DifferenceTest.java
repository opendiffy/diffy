package ai.diffy.compare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DifferenceTest {
    private static final Logger log = LoggerFactory.getLogger(DifferenceTest.class);

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[\"a\"]", "[\"a\",\"b\"]", "[1,1,2]", "[[1,2],[]]", "[null,true,1,\"a\"]"})
    void identicalJsonArraysHaveNoDifference(String json) {
        Map<String, Difference> differences = Difference.apply(json, json).flattened();

        assertInstanceOf(NoDifference.class, differences.get("value.NoDifference"));
        assertTrue(differences.values().stream().allMatch(difference -> difference instanceof NoDifference));
    }

    @Test
    void identicalNativeArraysHaveNoDifference() {
        Difference difference = Difference.apply(new String[]{"a", "b"}, new String[]{"a", "b"});

        assertInstanceOf(NoDifference.class, difference);
    }

    @Test
    void identicalNestedArraysRetainFieldLevelNoDifferences() {
        String json = "{\"name\":\"same\",\"content\":[],\"options\":{\"values\":[1,2]}}";
        Map<String, Difference> differences = Difference.apply(json, json).flattened();

        assertInstanceOf(NoDifference.class, differences.get("value.name.NoDifference"));
        assertInstanceOf(NoDifference.class, differences.get("value.content.NoDifference"));
        assertInstanceOf(NoDifference.class, differences.get("value.options.values.NoDifference"));
        assertTrue(differences.values().stream().allMatch(difference -> difference instanceof NoDifference));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "[1,2]|[2,1]|OrderingDifference",
            "[1,1,2]|[1,2,1]|OrderingDifference",
            "[1,2]|[1,3]|PrimitiveDifference",
            "[1,2]|[1,2,3]|SeqSizeDifference",
            "[1,2]|[1]|SeqSizeDifference",
            "{\"content\":[1,2]}|{\"content\":[2,1]}|content.OrderingDifference"
    }, delimiter = '|')
    void actualArrayDifferencesArePreserved(String left, String right, String field) {
        Map<String, Difference> differences = Difference.apply(left, right).flattened();

        assertTrue(differences.containsKey("value." + field), differences::toString);
    }

    @Test
    public void payloadTest() throws IOException {
        FileSystemResource payload = new FileSystemResource("src/test/resources/payload.json");
        String json = FileCopyUtils.copyToString(new InputStreamReader(payload.getInputStream()));
        log.info(json);
        Difference diff = Difference.apply(json, json);
        diff.flattened().forEach((key, value) -> {
            log.info("{} - {}", key, value);
        });
    }
}
