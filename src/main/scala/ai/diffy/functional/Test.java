package ai.diffy.functional;

import ai.diffy.functional.endpoints.Endpoint;
import ai.diffy.functional.topology.InvocationLogger;

public class Test {
    public static void main(String [] args){
        Endpoint<String,String> repeat = Endpoint.from("toUpper", () -> (str)->(str+str));
        System.out.println(repeat.apply("lower"));
        Endpoint<String, String> repeatlower = repeat.withMiddleware(apply -> (str) -> str.toLowerCase());
        System.out.println(repeatlower.apply("UPPER"));
        Endpoint<String, String> composed = Endpoint.from("composed", repeat, repeatlower, (_upper, _lower) -> _upper.andThen(_lower));
        System.out.println(composed.apply("UppeR"));
        System.out.println(
            composed.deepTransform(InvocationLogger::mapper)
            .apply("bubbles are BIG"));
    }
}
