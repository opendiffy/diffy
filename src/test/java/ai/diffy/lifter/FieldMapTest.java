package ai.diffy.lifter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldMapTest {

    @Test
    void fieldMapsWithEqualValuesAreEqual() {
        Map<String, Object> left = new LinkedHashMap<>();
        left.put("a", 1);
        left.put("b", List.of("x"));
        Map<String, Object> right = new LinkedHashMap<>();
        right.put("b", List.of("x"));
        right.put("a", 1);

        assertEquals(new FieldMap(left), new FieldMap(right));
        assertEquals(new FieldMap(left).hashCode(), new FieldMap(right).hashCode());
    }

    @Test
    void fieldMapsWithDifferentValuesAreNotEqual() {
        assertNotEquals(new FieldMap(Map.of("a", 1)), new FieldMap(Map.of("a", 2)));
        assertNotEquals(new FieldMap(Map.of("a", 1)), new FieldMap(Map.of("b", 1)));
    }
}
