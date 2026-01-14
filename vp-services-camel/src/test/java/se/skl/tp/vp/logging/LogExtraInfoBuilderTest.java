package se.skl.tp.vp.logging;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

public class LogExtraInfoBuilderTest {
    @Test
    void testFilterHeaders() {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("X-Vp-Auth-Cert", "This-should-be-hidden");
        headers.put("x-fk-auth-cert", "This-should-be-hidden");
        headers.put("X-Fk-Auth-Cert", "This-should-be-hidden");
        headers.put("X-Forwarded-Tls-Client-Cert", "This-should-be-hidden");
        
        Map<String, String> result = new HashMap<String, String>();
        LogExtraInfoBuilder.filterHeaders(headers, result);

        assertAll(() -> result.values().forEach(str -> assertEquals(LogExtraInfoBuilder.FILTERED_TEXT, str)));
    }
}
