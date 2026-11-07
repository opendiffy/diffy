package ai.diffy.compare;

import ai.diffy.lifter.FieldMap;
import ai.diffy.lifter.JsonLifter;
import ai.diffy.lifter.StringLifter;
import ai.diffy.util.Memoize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Java 21 port of the Scala Difference ADT.
 *
 * All subtypes live in this package.  Sealed interfaces/classes work because
 * all permits-listed types are in the same package and compiled together.
 */
public interface Difference {

    Map<String, Difference> flattened();

    default Map<String, Object> toMap() {
        return Map.of("type", getClass().getSimpleName());
    }

    Logger log = LoggerFactory.getLogger(Difference.class);

    // =========================================================================
    // Static helpers
    // =========================================================================

    static boolean isPrimitive(Object o) {
        return o instanceof Boolean
            || o instanceof Byte
            || o instanceof Character
            || o instanceof Short
            || o instanceof Integer
            || o instanceof Long
            || o instanceof Float
            || o instanceof Double
            || o instanceof String
            || o instanceof Enum<?>;
    }

    static Map<String, Object> mkMap(Object obj) {
        return MapMakerHolder.INSTANCE.apply(obj.getClass()).apply(obj);
    }

    static Object lift(Object a) {
        if (a instanceof Object[] arr) return Arrays.asList(arr);
        if (a instanceof boolean[] arr) {
            List<Boolean> list = new ArrayList<>();
            for (boolean b : arr) list.add(b);
            return list;
        }
        if (a instanceof byte[] arr) {
            List<Byte> list = new ArrayList<>();
            for (byte b : arr) list.add(b);
            return list;
        }
        if (a instanceof int[] arr) {
            List<Integer> list = new ArrayList<>();
            for (int b : arr) list.add(b);
            return list;
        }
        if (a instanceof long[] arr) {
            List<Long> list = new ArrayList<>();
            for (long b : arr) list.add(b);
            return list;
        }
        if (a instanceof ByteBuffer bb) return new String(bb.array());
        if (a instanceof com.fasterxml.jackson.databind.JsonNode jn) return JsonLifter.lift(jn);
        if (a instanceof String s) return StringLifter.lift(s);
        if (a == null) return JsonLifter.JsonNull.INSTANCE;
        return a;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Difference apply(Object leftRaw, Object rightRaw) {
        Object l = lift(leftRaw);
        Object r = lift(rightRaw);

        if (isPrimitive(l) && Objects.equals(l, r)) return new NoDifference<>(l);
        if (isPrimitive(l) && l.getClass() == r.getClass()) return new PrimitiveDifference<>(l, r);
        if (l instanceof List<?> ls && r instanceof List<?> rs && ls.equals(rs)) return new NoDifference<>(ls);
        if (l instanceof List<?> ls && r instanceof List<?> rs) return diffSeq((List<Object>) ls, (List<Object>) rs);
        if (l instanceof Set<?> ls && r instanceof Set<?> rs) return diffSet((Set<Object>) ls, (Set<Object>) rs);
        if (l instanceof FieldMap lm && r instanceof FieldMap rm) return diffObjectMap(lm, rm);
        if (l instanceof Map<?, ?> lm && r instanceof Map<?, ?> rm) return diffMap((Map<Object, Object>) lm, (Map<Object, Object>) rm);
        if (l.getClass() != r.getClass()) return new TypeDifference<>(l, r);
        return diffObject(l, r);
    }

    static <A> TerminalDifference diffSet(Set<A> left, Set<A> right) {
        if (left.equals(right)) return new NoDifference<>(left);
        Set<A> lnr = new HashSet<>(left);
        lnr.removeAll(right);
        Set<A> rnl = new HashSet<>(right);
        rnl.removeAll(left);
        return new SetDifference<>(lnr, rnl);
    }

    @SuppressWarnings("unchecked")
    static <A> SeqDifference diffSeq(List<A> left, List<A> right) {
        List<A> leftNotRight = new ArrayList<>(left);
        leftNotRight.removeAll(right);
        List<A> rightNotLeft = new ArrayList<>(right);
        rightNotLeft.removeAll(left);

        if (leftNotRight.isEmpty() && rightNotLeft.isEmpty()) {
            List<Integer> lPattern = left.stream().map(left::indexOf).collect(Collectors.toList());
            List<Integer> rPattern = right.stream().map(left::indexOf).collect(Collectors.toList());
            return new OrderingDifference(lPattern, rPattern);
        } else if (left.size() == right.size()) {
            List<Difference> diffs = new ArrayList<>();
            for (int i = 0; i < left.size(); i++) {
                diffs.add(apply(left.get(i), right.get(i)));
            }
            return new IndexedDifference(diffs);
        } else {
            return new SeqSizeDifference<>(leftNotRight, rightNotLeft);
        }
    }

    @SuppressWarnings("unchecked")
    static <A> MapDifference<A> diffMap(Map<A, Object> lm, Map<A, Object> rm) {
        TerminalDifference keysDiff = diffSet(lm.keySet(), rm.keySet());
        Set<A> shared = new HashSet<>(lm.keySet());
        shared.retainAll(rm.keySet());
        Map<A, Difference> valueDiffs = new LinkedHashMap<>();
        for (A key : shared) {
            valueDiffs.put(key, apply(lm.get(key), rm.get(key)));
        }
        return new MapDifference<>(keysDiff, valueDiffs);
    }

    static ObjectDifference diffObjectMap(FieldMap lm, FieldMap rm) {
        return new ObjectDifference(diffMap(
            new LinkedHashMap<>(lm.value),
            new LinkedHashMap<>(rm.value)
        ));
    }

    static ObjectDifference diffObject(Object left, Object right) {
        return new ObjectDifference(diffMap(mkMap(left), mkMap(right)));
    }
}
