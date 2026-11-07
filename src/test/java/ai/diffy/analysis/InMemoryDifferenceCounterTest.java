package ai.diffy.analysis;

import ai.diffy.compare.Difference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryDifferenceCounterTest {

    @ParameterizedTest
    @ValueSource(strings = {"{\"content\":[]}", "{\"content\":[1]}", "{\"content\":[1,2]}", "{\"content\":[[1,2],[]]}"})
    void identicalArraysDoNotIncrementDifferentRequests(String json) {
        InMemoryDifferenceCounter counter = new InMemoryDifferenceCounter("identical-array-test");
        counter.count("arrays", Difference.apply(json, json).flattened());

        assertEquals(1, counter.endpoint("arrays").total());
        assertEquals(0, counter.endpoint("arrays").differences());
        assertTrue(counter.fields("arrays").containsKey("value.content.NoDifference"));
    }

    @Test
    void actualOrderingDifferencesStillIncrementDifferentRequests() {
        InMemoryDifferenceCounter counter = new InMemoryDifferenceCounter("reordered-array-test");
        counter.count("arrays", Difference.apply("{\"content\":[1,2]}", "{\"content\":[2,1]}").flattened());

        assertEquals(1, counter.endpoint("arrays").total());
        assertEquals(1, counter.endpoint("arrays").differences());
        assertTrue(counter.fields("arrays").containsKey("value.content.OrderingDifference"));
    }
}
