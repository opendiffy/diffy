package ai.diffy.functional.algebra;

public class BijectionTest {
    public static void main(String[] args) {
        Bijection<String, String> identity = Bijection.of(String::toUpperCase, String::toLowerCase);
        try {
            identity.apply("s");
            identity.unapply("s");
            String x = identity.wrap(str -> str+str).apply("lower");
            System.out.println(x);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}