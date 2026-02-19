package se.skl.tp.vp.logging.old;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.util.TestLogAppender;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LegacyMessageInfoLogger.
 */
@SuppressWarnings({"removal"}) // LegacyMessageInfoLogger is deprecated, but we need to test it until it's removed.
class LegacyMessageInfoLoggerTest {

    private LegacyMessageInfoLogger messageLogger;
    private Exchange exchange;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        messageLogger = new LegacyMessageInfoLogger();
        camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "test-correlation-id");
        exchange.setProperty(VPExchangeProperties.SENDER_ID, "test-sender-id");
        exchange.setProperty(VPExchangeProperties.RECEIVER_ID, "test-receiver-id");
        TestLogAppender.clearEvents();
    }

    @Test
    void testLogReqInLogsMessage() {
        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertEquals(1, events.size(), "Should log one req-in event");
        LogEvent event = events.get(0);
        assertTrue(event.getLevel() == Level.DEBUG || event.getLevel() == Level.INFO,
                "Log level should be DEBUG or INFO");
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("req-in"), "Log message should contain message type");
    }

    @Test
    void testLogReqOutLogsMessage() {
        messageLogger.logReqOut(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_OUT);
        assertEquals(1, events.size(), "Should log one req-out event");
        LogEvent event = events.get(0);
        assertTrue(event.getLevel() == Level.DEBUG || event.getLevel() == Level.INFO,
                "Log level should be DEBUG or INFO");
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("req-out"), "Log message should contain message type");
    }

    @Test
    void testLogRespInLogsMessage() {
        messageLogger.logRespIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_IN);
        assertEquals(1, events.size(), "Should log one resp-in event");
        LogEvent event = events.get(0);
        assertTrue(event.getLevel() == Level.DEBUG || event.getLevel() == Level.INFO,
                "Log level should be DEBUG or INFO");
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("resp-in"), "Log message should contain message type");
    }

    @Test
    void testLogRespOutLogsMessage() {
        messageLogger.logRespOut(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_OUT);
        assertEquals(1, events.size(), "Should log one resp-out event");
        LogEvent event = events.get(0);
        assertTrue(event.getLevel() == Level.DEBUG || event.getLevel() == Level.INFO,
                "Log level should be DEBUG or INFO");
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("resp-out"), "Log message should contain message type");
    }

    @Test
    void testLogErrorWithException() {
        String stackTrace = "java.lang.RuntimeException: Test error\n\tat TestClass.method(TestClass.java:10)";
        RuntimeException exception = new RuntimeException("Test error");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);

        messageLogger.logError(exchange, stackTrace);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_ERROR);
        assertEquals(1, events.size(), "Should log one error event");
        LogEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        String message = event.getMessage().getFormattedMessage();
        assertNotNull(message);
        assertTrue(message.contains("error"), "Error message should contain 'error' type");
        assertTrue(message.contains("Stacktrace"), "Error message should contain stack trace");
    }

    @Test
    void testLogErrorWithoutException() {
        String stackTrace = "Stack trace information";

        messageLogger.logError(exchange, stackTrace);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_ERROR);
        assertEquals(1, events.size(), "Should log one error event even without exception");
        LogEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
    }

    @Test
    void testLogErrorHandlesExceptionDuringLogging() {
        Exchange emptyExchange = new DefaultExchange(camelContext);

        assertDoesNotThrow(() -> messageLogger.logError(emptyExchange, "stack trace"));

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_ERROR);
        assertFalse(events.isEmpty(), "Should log error event or fallback error message");
    }

    @Test
    void testLogWithDebugEnabledIncludesPayload() {
        String payload = "<soap:Envelope>test payload</soap:Envelope>";
        exchange.getIn().setBody(payload);
        Logger logger = LogManager.getLogger(MessageLogger.REQ_IN);

        messageLogger.log(logger, exchange, "req-in");

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        LogEvent event = events.get(0);
        if (event.getLevel() == Level.DEBUG) {
            String message = event.getMessage().getFormattedMessage();
            assertTrue(message.contains(payload), "Debug log should include payload");
        }
    }

    @Test
    void testLogWithInfoEnabledDoesNotIncludePayload() {
        String payload = "<soap:Envelope>sensitive data</soap:Envelope>";
        exchange.getIn().setBody(payload);

        // Get the logger and set it to INFO level (not DEBUG)
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.RESP_OUT);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.INFO);

            messageLogger.log(logger, exchange, "resp-out");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_OUT);
            assertFalse(events.isEmpty(), "Should log event");
            LogEvent event = events.get(0);
            assertEquals(Level.INFO, event.getLevel(), "Should log at INFO level");
            String message = event.getMessage().getFormattedMessage();
            assertFalse(message.contains(payload),
                "INFO log should NOT include payload");
            assertTrue(message.contains("resp-out"),
                "INFO log should include message type");
        } finally {
            // Restore original level
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogHandlesExceptionDuringLogging() {
        Exchange problematicExchange = new DefaultExchange(camelContext);
        Logger logger = LogManager.getLogger(MessageLogger.REQ_IN);

        assertDoesNotThrow(() -> messageLogger.log(logger, problematicExchange, "req-in"));
    }

    @Test
    void testCompleteRequestResponseFlow() {
        messageLogger.logReqIn(exchange);
        messageLogger.logReqOut(exchange);
        messageLogger.logRespIn(exchange);
        messageLogger.logRespOut(exchange);

        assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_IN));
        assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_OUT));
        assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_IN));
        assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_OUT));
    }

    @Test
    void testLogReqInWithMinimalExchange() {
        Exchange minimalExchange = new DefaultExchange(camelContext);
        minimalExchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());

        assertDoesNotThrow(() -> messageLogger.logReqIn(minimalExchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event with minimal exchange setup");
    }

    @Test
    void testLogReqOutWithMinimalExchange() {
        Exchange minimalExchange = new DefaultExchange(camelContext);
        minimalExchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());

        assertDoesNotThrow(() -> messageLogger.logReqOut(minimalExchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_OUT);
        assertFalse(events.isEmpty(), "Should log event with minimal exchange setup");
    }

    @Test
    void testLogRespInWithMinimalExchange() {
        Exchange minimalExchange = new DefaultExchange(camelContext);
        minimalExchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());

        assertDoesNotThrow(() -> messageLogger.logRespIn(minimalExchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_IN);
        assertFalse(events.isEmpty(), "Should log event with minimal exchange setup");
    }

    @Test
    void testLogRespOutWithMinimalExchange() {
        Exchange minimalExchange = new DefaultExchange(camelContext);
        minimalExchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());

        assertDoesNotThrow(() -> messageLogger.logRespOut(minimalExchange));

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_OUT);
        assertFalse(events.isEmpty(), "Should log event with minimal exchange setup");
    }

    @Test
    void testMultipleErrorLogs() {
        String stackTrace1 = "Error trace 1";
        String stackTrace2 = "Error trace 2";

        messageLogger.logError(exchange, stackTrace1);
        messageLogger.logError(exchange, stackTrace2);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_ERROR);
        assertEquals(2, events.size(), "Should log multiple error events");
    }

    @ParameterizedTest(name = "Log message should contain {0}")
    @MethodSource("logMessageContentProvider")
    void testLogMessageContainsExpectedContent(String description, String expectedContent) {
        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains(expectedContent),
                "Log message should contain " + description);
    }

    static Stream<Arguments> logMessageContentProvider() {
        return Stream.of(
                Arguments.of("source class name", "LegacyMessageInfoLogger"),
                Arguments.of("correlation ID", "test-correlation-id"),
                Arguments.of("sender ID", "test-sender-id"),
                Arguments.of("receiver ID", "test-receiver-id")
        );
    }

    @Test
    void testLogErrorContainsExceptionClass() {
        RuntimeException exception = new RuntimeException("Test error");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
        String stackTrace = "java.lang.RuntimeException: Test error\n\tat TestClass.method(TestClass.java:10)";

        messageLogger.logError(exchange, stackTrace);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_ERROR);
        assertEquals(1, events.size(), "Should log one error event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("RuntimeException"),
                "Error log should contain exception class");
    }

    @Test
    void testLogWithHttpUrlProperty() {
        exchange.setProperty(VPExchangeProperties.HTTP_URL_IN, "http://test-endpoint/service");

        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("http://test-endpoint/service"),
                "Log message should contain endpoint URL");
    }

    @Test
    void testLogMessageContainsMessageId() {
        String messageId = exchange.getMessage().getMessageId();

        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains(messageId),
                "Log message should contain message ID");
    }

    @Test
    void testLogMessageContainsComponentId() {
        String componentId = camelContext.getName();

        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains(componentId),
                "Log message should contain component ID");
    }

    @Test
    void testLogMessageFormat() {
        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertFalse(events.isEmpty(), "Should log event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("skltp-messages"), "Log message should contain skltp-messages marker");
        assertTrue(message.contains("LogMessage="), "Log message should contain LogMessage field");
        assertTrue(message.contains("BusinessCorrelationId="), "Log message should contain BusinessCorrelationId field");
        assertTrue(message.contains("ExtraInfo="), "Log message should contain ExtraInfo field");
    }

    @Test
    void testLogMethodWithDebugLevelIncludesPayload() {
        String payload = "<soap:Envelope>debug payload data</soap:Envelope>";
        exchange.getIn().setBody(payload);

        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.REQ_IN);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.DEBUG);

            messageLogger.log(logger, exchange, "req-in");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
            assertEquals(1, events.size(), "Should log one event");
            LogEvent event = events.get(0);
            assertEquals(Level.DEBUG, event.getLevel(), "Should log at DEBUG level");
            String message = event.getMessage().getFormattedMessage();
            assertTrue(message.contains(payload),
                "DEBUG log should include payload");
            assertTrue(message.contains("logEvent-debug"),
                "DEBUG log should contain debug event marker");
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogMethodWithInfoLevelExcludesPayload() {
        String payload = "<soap:Envelope>sensitive info payload</soap:Envelope>";
        exchange.getIn().setBody(payload);

        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.REQ_OUT);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.INFO);

            messageLogger.log(logger, exchange, "req-out");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_OUT);
            assertEquals(1, events.size(), "Should log one event");
            LogEvent event = events.get(0);
            assertEquals(Level.INFO, event.getLevel(), "Should log at INFO level");
            String message = event.getMessage().getFormattedMessage();
            assertFalse(message.contains(payload),
                "INFO log should NOT include payload");
            assertTrue(message.contains("logEvent-info"),
                "INFO log should contain info event marker");
            assertTrue(message.contains("req-out"),
                "INFO log should include message type");
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogMethodWithWarnLevelDoesNotLog() {
        String payload = "<soap:Envelope>warn level payload</soap:Envelope>";
        exchange.getIn().setBody(payload);

        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.RESP_IN);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.WARN);
            TestLogAppender.clearEvents();

            messageLogger.log(logger, exchange, "resp-in");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_IN);
            assertEquals(0, events.size(),
                "Should not log when logger level is WARN (neither DEBUG nor INFO is enabled)");
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogMethodInfoLevelContainsExtraInfo() {
        exchange.setProperty(VPExchangeProperties.SENDER_ID, "info-sender");
        exchange.setProperty(VPExchangeProperties.RECEIVER_ID, "info-receiver");
        exchange.setProperty(VPExchangeProperties.VAGVAL, "http://info-endpoint/service");

        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.RESP_OUT);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.INFO);

            messageLogger.log(logger, exchange, "resp-out");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_OUT);
            assertEquals(1, events.size(), "Should log one event");
            LogEvent event = events.get(0);
            assertEquals(Level.INFO, event.getLevel(), "Should log at INFO level");
            String message = event.getMessage().getFormattedMessage();
            assertTrue(message.contains("info-sender"), "INFO log should contain sender ID");
            assertTrue(message.contains("info-receiver"), "INFO log should contain receiver ID");
            assertTrue(message.contains("http://info-endpoint/service"),
                "INFO log should contain endpoint URL");
            assertTrue(message.contains("LegacyMessageInfoLogger"),
                "INFO log should contain source class name");
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogMethodDebugLevelContainsExtraInfoAndPayload() {
        String payload = "<soap:Envelope>complete debug data</soap:Envelope>";
        exchange.getIn().setBody(payload);
        exchange.setProperty(VPExchangeProperties.SENDER_ID, "debug-sender");
        exchange.setProperty(VPExchangeProperties.RECEIVER_ID, "debug-receiver");

        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MessageLogger.REQ_IN);
        org.apache.logging.log4j.Level originalLevel = logger.getLevel();

        try {
            logger.setLevel(org.apache.logging.log4j.Level.DEBUG);

            messageLogger.log(logger, exchange, "req-in");

            List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
            assertEquals(1, events.size(), "Should log one event");
            LogEvent event = events.get(0);
            assertEquals(Level.DEBUG, event.getLevel(), "Should log at DEBUG level");
            String message = event.getMessage().getFormattedMessage();
            assertTrue(message.contains(payload), "DEBUG log should contain payload");
            assertTrue(message.contains("debug-sender"), "DEBUG log should contain sender ID");
            assertTrue(message.contains("debug-receiver"), "DEBUG log should contain receiver ID");
            assertTrue(message.contains("LegacyMessageInfoLogger"),
                "DEBUG log should contain source class name");
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void testLogWithSoapFaultFields() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:Server");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, "Internal server error");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, "Detailed error information");

        messageLogger.logRespOut(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_OUT);
        assertEquals(1, events.size(), "Should log one event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("faultCode=soap:Server"),
                "Log should contain SOAP fault code");
        assertTrue(message.contains("faultString=Internal server error"),
                "Log should contain SOAP fault string");
        assertTrue(message.contains("faultDetail=Detailed error information"),
                "Log should contain SOAP fault detail");
    }

    @Test
    void testLogWithSoapFaultCodeOnly() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:Client");

        messageLogger.logRespIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.RESP_IN);
        assertEquals(1, events.size(), "Should log one event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("faultCode=soap:Client"),
                "Log should contain SOAP fault code");
        assertFalse(message.contains("faultString="),
                "Log should not contain SOAP fault string when not set");
        assertFalse(message.contains("faultDetail="),
                "Log should not contain SOAP fault detail when not set");
    }

    @Test
    void testLogWithSoapFaultPartialFields() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:VersionMismatch");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, "SOAP version mismatch");

        messageLogger.logReqOut(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_OUT);
        assertEquals(1, events.size(), "Should log one event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertTrue(message.contains("faultCode=soap:VersionMismatch"),
                "Log should contain SOAP fault code");
        assertTrue(message.contains("faultString=SOAP version mismatch"),
                "Log should contain SOAP fault string");
        assertFalse(message.contains("faultDetail="),
                "Log should not contain SOAP fault detail when not set");
    }

    @Test
    void testLogWithoutSoapFaultFields() {
        messageLogger.logReqIn(exchange);

        List<LogEvent> events = TestLogAppender.getEvents(MessageLogger.REQ_IN);
        assertEquals(1, events.size(), "Should log one event");
        String message = events.get(0).getMessage().getFormattedMessage();
        assertFalse(message.contains("faultCode="),
                "Log should not contain SOAP fault code when not set");
        assertFalse(message.contains("faultString="),
                "Log should not contain SOAP fault string when not set");
        assertFalse(message.contains("faultDetail="),
                "Log should not contain SOAP fault detail when not set");
    }

}


