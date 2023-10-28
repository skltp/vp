package se.skl.tp.vp.logging;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.logentry.LogEntry;
import se.skl.tp.vp.logging.logentry.LogMessageType;
import se.skl.tp.vp.logging.logentry.LogMetadataInfoType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    @Test
    public void testObjectMessageLog() throws Exception {
        Logger logger = mock(Logger.class);
        Exchange exchange = mock(Exchange.class);
        Message in = mock(Message.class);
        CamelContext ctx = mock(CamelContext.class);

        Mockito.when(exchange.getProperty(VPExchangeProperties.EXCHANGE_CREATED, Date.class)).thenReturn(new Date());
        Mockito.when(exchange.getProperty(anyString(), eq(String.class))).thenReturn("mockedString");
        Mockito.when(exchange.getMessage()).thenReturn(in);
        Mockito.when(exchange.getIn()).thenReturn(in);
        Mockito.when(exchange.getContext()).thenReturn(ctx);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);

        MessageInfoLogger mil = new MessageInfoLogger();
        Assertions.assertThrows(Exception.class, () -> {
            mil.objLog(null, exchange, null);
        });
        mil.objLog(logger, null, "testObjectLogger");
        Logger logger2 = mock(Logger.class);
        Mockito.when(logger2.isDebugEnabled()).thenReturn(true);
        mil.objLog(logger2, exchange, "testObjectLogger");
        assertNotNull(logger2);

        LogEntry logEntry = new LogEntry();
        logEntry.setPayload(exchange.getIn().getBody(String.class));
        Map<String, Object> msgMap = new HashMap<>();
        logEntry.setMessageInfo(new LogMessageType());
        LogMessageFormatter.format("logEvent-test", logEntry, msgMap);

        assertNotNull(msgMap);
        assertEquals("logEvent-test", msgMap.get("logEventType"));
        LogMessageType lmt = (LogMessageType) msgMap.get("messageInfo");
        assertNull("messageinfo.message should be null", lmt.getMessage());
        assertNull("messageinfo.exception should be null", lmt.getException());
    }
}
