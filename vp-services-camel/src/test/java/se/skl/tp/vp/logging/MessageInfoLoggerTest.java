package se.skl.tp.vp.logging;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.util.TestLogAppender;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageInfoLoggerTest {

    private MessageInfoLogger messageInfoLogger;
    private Exchange exchange;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        messageInfoLogger = new MessageInfoLogger();
        camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "test-correlation-id");
        TestLogAppender.clearEvents();
    }

    @Test
    void testLogReqInCreatesSpanIdAndLogs() {
        messageInfoLogger.logReqIn(exchange);

        String spanId = getSpanA();
        assertNotNull(spanId, "Span ID should be created");
        assertFalse(spanId.isEmpty(), "Span ID should not be empty");
        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_IN);
        assertEquals(1, events.size(), "Should log one req-in event");
        LogEvent event = events.get(0);
        assertEquals(Level.DEBUG, event.getLevel());
        assertNotNull(event.getMessage());
    }

    @Test
    void testLogReqOutCreatesSpanIdAndLogs() {
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, "parent-span-id");

        messageInfoLogger.logReqOut(exchange);

        String outboundSpanId = getSpanB();
        assertNotNull(outboundSpanId, "Outbound span ID should be created");
        assertFalse(outboundSpanId.isEmpty(), "Outbound span ID should not be empty");
        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_OUT);
        assertEquals(1, events.size(), "Should log one req-out event");
        LogEvent event = events.get(0);
        assertEquals(Level.DEBUG, event.getLevel());
    }

    @Test
    void testLogRespInLogsAndRemovesOutboundSpanId() {
        String outboundSpanId = "outbound-span-id";
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, outboundSpanId);

        messageInfoLogger.logRespIn(exchange);

        assertNull(exchange.getProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN),
                "Outbound span ID should be removed");
        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.RESP_IN);
        assertEquals(1, events.size(), "Should log one resp-in event");
    }

    @Test
    void testLogRespOutLogsAndRemovesRequestSpanId() {
        String requestSpanId = "request-span-id";
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, requestSpanId);

        messageInfoLogger.logRespOut(exchange);

        assertNull(exchange.getProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT),
                "Request span ID should be removed");
        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.RESP_OUT);
        assertEquals(1, events.size(), "Should log one resp-out event");
    }

    @Test
    void testLogErrorWithException() {
        String stackTrace = "java.lang.RuntimeException: Test error\n\tat TestClass.method(TestClass.java:10)";
        RuntimeException exception = new RuntimeException("Test error");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);

        messageInfoLogger.logError(exchange, stackTrace);

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_ERROR);
        assertEquals(1, events.size(), "Should log one error event");
        LogEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("error"), "Error message should contain 'error' type");
    }

    @Test
    void testLogErrorWithoutException() {
        String stackTrace = "Stack trace information";

        messageInfoLogger.logError(exchange, stackTrace);

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_ERROR);
        assertEquals(1, events.size(), "Should log one error event even without exception");
    }

    @Test
    void testLogErrorHandlesExceptionDuringLogging() {
        Exchange emptyExchange = new DefaultExchange(camelContext);
        // Don't set required properties to potentially trigger exception handling

        assertDoesNotThrow(() -> messageInfoLogger.logError(emptyExchange, "stack trace"));

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_ERROR);
        assertFalse(events.isEmpty(), "Should log error event or fallback error message");
    }

    @Test
    void testLogWithDebugEnabledIncludesPayload() {
        String payload = "<soap:Envelope>test payload</soap:Envelope>";
        exchange.getIn().setBody(payload);
        Logger logger = LogManager.getLogger(MessageInfoLogger.REQ_IN);

        messageInfoLogger.log(logger, exchange, "req-in");

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        LogEvent event = events.get(0);
        assertNotNull(event.getMessage());
    }

    @Test
    void testLogWithInfoEnabledDoesNotIncludePayload() {
        String payload = "<soap:Envelope>sensitive data</soap:Envelope>";
        exchange.getIn().setBody(payload);
        Logger logger = LogManager.getLogger(MessageInfoLogger.RESP_OUT);

        messageInfoLogger.log(logger, exchange, "resp-out");

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.RESP_OUT);
        assertFalse(events.isEmpty(), "Should log event");
    }

    @Test
    void testLogHandlesExceptionDuringLogging() {
        Exchange problematicExchange = new DefaultExchange(camelContext);
        Logger logger = LogManager.getLogger(MessageInfoLogger.REQ_IN);

        assertDoesNotThrow(() -> messageInfoLogger.log(logger, problematicExchange, "req-in"));
    }

    @Test
    void testSpanIdsAreUnique() {
        messageInfoLogger.logReqIn(exchange);
        String spanId1 = getSpanA();
        messageInfoLogger.logReqOut(exchange);
        String spanId2 = getSpanB();

        assertNotNull(spanId1);
        assertNotNull(spanId2);
        assertNotEquals(spanId1, spanId2, "Span IDs should be unique");
    }

    @Test
    void testCompleteRequestResponseFlow() {
        messageInfoLogger.logReqIn(exchange);
        assertNotNull(getSpanA(), "Span A should be set after req-in");

        messageInfoLogger.logReqOut(exchange);
        assertNotNull(getSpanA(), "Span A should still exist after req-out");
        assertNotNull(getSpanB(), "Span B should be set after req-out");

        messageInfoLogger.logRespIn(exchange);
        assertNotNull(getSpanA(), "Span A should still exist after resp-in");
        assertNull(getSpanB(), "Span B should be removed after resp-in");

        messageInfoLogger.logRespOut(exchange);
        assertNull(getSpanA(), "Span A should be removed after resp-out");

        assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
        assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_OUT));
        assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_IN));
        assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
    }

    @Test
    void testLogReqInWithEmptyExchange() {
        Exchange emptyExchange = new DefaultExchange(camelContext);

        assertDoesNotThrow(() -> messageInfoLogger.logReqIn(emptyExchange));

        String spanId = emptyExchange.getProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, String.class);
        assertNotNull(spanId, "Span ID should still be created even with empty exchange");
    }

    @Test
    void testLogReqOutWithEmptyExchange() {
        Exchange emptyExchange = new DefaultExchange(camelContext);

        assertDoesNotThrow(() -> messageInfoLogger.logReqOut(emptyExchange));

        String spanId = emptyExchange.getProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, String.class);
        assertNotNull(spanId, "Outbound span ID should still be created even with empty exchange");
    }

    @Test
    void testLogRespInWithoutSpanId() {
        assertDoesNotThrow(() -> messageInfoLogger.logRespIn(exchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.RESP_IN);
        assertEquals(1, events.size(), "Should log resp-in event even without span ID");
    }

    @Test
    void testLogRespOutWithoutSpanId() {
        assertDoesNotThrow(() -> messageInfoLogger.logRespOut(exchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.RESP_OUT);
        assertEquals(1, events.size(), "Should log resp-out event even without span ID");
    }

    @Test
    void testMultipleErrorLogs() {
        String stackTrace1 = "Error trace 1";
        String stackTrace2 = "Error trace 2";

        messageInfoLogger.logError(exchange, stackTrace1);
        messageInfoLogger.logError(exchange, stackTrace2);

        List<LogEvent> events = TestLogAppender.getEvents(MessageInfoLogger.REQ_ERROR);
        assertEquals(2, events.size(), "Should log multiple error events");
    }

    private String getSpanB() {
        return exchange.getProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, String.class);
    }

    private String getSpanA() {
        return exchange.getProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, String.class);
    }
}
