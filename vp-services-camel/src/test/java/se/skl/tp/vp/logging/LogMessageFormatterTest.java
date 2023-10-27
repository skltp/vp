package se.skl.tp.vp.logging;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.logging.logentry.LogEntry;
import se.skl.tp.vp.logging.logentry.LogMessageType;
import se.skl.tp.vp.logging.logentry.LogMetadataInfoType;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.wildfly.common.Assert.assertNotNull;

@CamelSpringBootTest
@SpringBootTest(properties = { "message.logger.method=object" })
@DirtiesContext
public class LogMessageFormatterTest {

    @Test
    public void testFormatWithObject() throws Exception {

        String logEventType = "testObjectMessage";
        String payload = "<Payload/>";

        LogEntry logEntry = new LogEntry();
        logEntry.setMessageInfo(new LogMessageType());
        logEntry.setMetadataInfo(new LogMetadataInfoType());
        logEntry.setExtraInfo(new HashMap<>());
        logEntry.setPayload(null);
        HashMap<String, Object> messageMap = new HashMap<>();
        LogMessageFormatter.format(logEventType, logEntry, messageMap);

        assertNotNull(messageMap);
        assertEquals(logEventType, messageMap.get("logEventType"));
        assertNull("payload should be null", messageMap.get("payload"));

        logEntry.setPayload(payload);
        LogMessageFormatter.format(logEventType, logEntry, messageMap);
        assertEquals(payload, messageMap.get("payload"));

    }
}
