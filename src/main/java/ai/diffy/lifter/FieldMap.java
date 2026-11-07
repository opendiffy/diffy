package ai.diffy.lifter;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class FieldMap {
    public final Map<String, Object> value;

    public FieldMap(Map<String, Object> value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof FieldMap other && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        // Match Scala: toSeq.sortBy key then toString
        return new TreeMap<>(value).toString();
    }
}
