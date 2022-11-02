package ai.diffy.interpreter;

public class SimpleTransformerTest {
    public static class Name{
        public String first;
        public String last;
    }
    public static void main(String[] args) {
        try {
            Name name = new Name();
            name.first = "Puneet";
            name.last = "Khanduri";
            Transformer<Name> transformer = new Transformer<>(Name.class, "(name)=>{console.log(JSON.stringify(name,null,4)); return name;}");
            transformer.apply(name);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
