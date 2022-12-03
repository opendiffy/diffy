package ai.diffy;

import io.netty.handler.codec.http.HttpHeaderValues;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpHeaderValuesTest {
    @Test
    public void urlEncodedHeader() {
        assertTrue(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.contentEquals("application/x-www-form-urlencoded"));
    }
}
