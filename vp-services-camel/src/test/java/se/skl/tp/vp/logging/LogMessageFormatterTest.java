package se.skl.tp.vp.logging;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.logentry.*;
import se.skl.tp.vp.util.JunitUtil;

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

    private Exchange exchange;
    private Logger infoLogger;
    private Logger debugLogger;
    private String logEventName;
    private String payload;
    private String stackTrace;
    private String messageType;
    private LogEntry logEntry;
    private MessageInfoLogger messageInfoLogger;

    @BeforeEach
    public void beforeEach() {
        infoLogger = mock(Logger.class);
        debugLogger = mock(Logger.class);
        exchange = mock(Exchange.class);
        messageInfoLogger = new MessageInfoLogger();
        logEventName = "logEvent-test";
        payload = "<Payload/>";
        stackTrace = "<testException/>";
        messageType = "testObjectLogger";
        logEntry = new LogEntry();
        logEntry.setMessageInfo(new LogMessageType());
        logEntry.setMetadataInfo(new LogMetadataInfoType());
        logEntry.setExtraInfo(new HashMap<>());
        logEntry.setRuntimeInfo(new LogRuntimeInfoType());

        Message in = mock(Message.class);
        CamelContext ctx = mock(CamelContext.class);

        Mockito.when(exchange.getProperty(VPExchangeProperties.EXCHANGE_CREATED, Date.class)).thenReturn(new Date());
        Mockito.when(exchange.getProperty(anyString(), eq(String.class))).thenReturn("mockedString");
        Mockito.when(exchange.getMessage()).thenReturn(in);
        Mockito.when(exchange.getIn()).thenReturn(in);
        Mockito.when(exchange.getContext()).thenReturn(ctx);
        Mockito.when(infoLogger.isDebugEnabled()).thenReturn(false);
        Mockito.when(debugLogger.isDebugEnabled()).thenReturn(true);


    }

    @Test
    public void testFormatWithObject() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        LogMessageFormatter.format(logEventName, logEntry, messageMap);

        assertNotNull(messageMap);
        assertEquals(logEventName, messageMap.get("logEventType"));
        assertNull("payload should be null", messageMap.get("payload"));

    }
    @Test
    public void testFormatWithString() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        logEntry.getMessageInfo().setException(new LogMessageExceptionType());
        logEntry.getMessageInfo().getException().setStackTrace(stackTrace);
        String logString = LogMessageFormatter.format(logEventName, logEntry);
        logEntry.setPayload(payload);
        String logStringWithPayload = LogMessageFormatter.format(logEventName, logEntry);

        assertNotNull(logString);
        assertNotNull(logStringWithPayload);
        JunitUtil.assertMatchRegexGroup(logString, "LogMessage=(.*)", "null", 1);
        JunitUtil.assertMatchRegexGroup(logString, "Stacktrace=(.*)", stackTrace, 1);

        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "LogMessage=(.*)", "null", 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "Stacktrace=(.*)", stackTrace, 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "Payload=(.*)", payload, 1);

        logEntry.setExtraInfo(null);
        String logStringWithoutExtra = LogMessageFormatter.format(logEventName, logEntry);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "ExtraInfo=(.*)", "", 1);

    }

    @Test
    public void testObjectMessageLog_() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        logEntry.setPayload(payload);
        logEntry.getMessageInfo().setException(new LogMessageExceptionType());
        logEntry.getMessageInfo().getException().setStackTrace(stackTrace);
        LogMessageFormatter.format(logEventName, logEntry, messageMap);
        assertEquals(payload, messageMap.get("payload"));
        assertEquals(stackTrace, messageMap.get("Stacktrace"));

    }

    @Test
    public void testObjectMessageLogNullLoggerThrowsException() throws Exception {
        Assertions.assertThrows(Exception.class, () -> {
            messageInfoLogger.objLog(null, exchange, null);
        });
    }
    @Test
    public void testObjectMessageLog2() throws Exception {
        messageInfoLogger.objLog(debugLogger, exchange, messageType);

        LogEntry logEntry = new LogEntry();
        logEntry.setPayload(payload);
        Map<String, Object> msgMap = new HashMap<>();
        logEntry.setMessageInfo(new LogMessageType());
        LogMessageFormatter.format(logEventName, logEntry, msgMap);

        assertNotNull(msgMap);
        assertEquals(logEventName, msgMap.get("logEventType"));
        LogMessageType lmt = (LogMessageType) msgMap.get("messageInfo");
        assertNull("messageinfo.message should be null", lmt.getMessage());
        assertNull("messageinfo.exception should be null", lmt.getException());
    }
}
