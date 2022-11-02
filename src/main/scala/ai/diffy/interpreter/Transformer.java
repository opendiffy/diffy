package ai.diffy.interpreter;

import org.graalvm.polyglot.Source;

public class Transformer<A> extends Lambda<A,A> {
    public Transformer(Class<A> clsResponse, String lambda) {
        super(clsResponse, lambda);
    }

    public Transformer(Class<A> clsResponse, Source lambda) {
        super(clsResponse, lambda);
    }
}
