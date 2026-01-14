package se.skl.tp.vp.logging;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.skl.tp.vp.logging.logentry.LogEntry;
import se.skl.tp.vp.logging.logentry.LogMessageExceptionType;
import se.skl.tp.vp.logging.logentry.LogMessageType;
import se.skl.tp.vp.utils.SoapFaultExtractor;
import se.skl.tp.vp.utils.SoapFaultInfo;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageInfoLoggerTest {

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private SoapFaultExtractor soapFaultExtractor;

    @Mock
    private Logger mockLogger;

    @InjectMocks
    private MessageInfoLogger messageInfoLogger;

    private MockedStatic<LogEntryBuilder> logEntryBuilderMock;
    private MockedStatic<LogMessageFormatter> logMessageFormatterMock;

    private LogEntry logEntry;
    private Map<String, String> extraInfo;

    @BeforeEach
    void setUp() {
        // Set up static mocks
        logEntryBuilderMock = mockStatic(LogEntryBuilder.class);
        logMessageFormatterMock = mockStatic(LogMessageFormatter.class);

        // Replace the static soapFaultExtractor with our mock
        MessageInfoLogger.soapFaultExtractor = soapFaultExtractor;

        // Set up common test data
        extraInfo = new HashMap<>();
        LogMessageType messageInfo = new LogMessageType();
        messageInfo.setMessage("test-message");

        logEntry = new LogEntry();
        logEntry.setExtraInfo(extraInfo);
        logEntry.setMessageInfo(messageInfo);

        // Common exchange setup - use lenient() to avoid UnnecessaryStubbingException
        // This allows tests that don't call exchange.getIn() to skip this stubbing
        lenient().when(exchange.getIn()).thenReturn(message);
        lenient().when(message.getBody(String.class)).thenReturn("test-body");
    }

    @AfterEach
    void tearDown() {
        logEntryBuilderMock.close();
        logMessageFormatterMock.close();
    }

    @Test
    void testLogReqIn() {
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("req-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format(anyString(), any(LogEntry.class)))
                .thenReturn("formatted-log-message");
        MessageInfoLogger loggerSpy = spy(messageInfoLogger);
        doNothing().when(loggerSpy).log(any(Logger.class), eq(exchange), eq("req-in"));

        loggerSpy.logReqIn(exchange);

        verify(loggerSpy).log(any(Logger.class), eq(exchange), eq("req-in"));
    }

    @Test
    void testLogReqOut() {
        MessageInfoLogger loggerSpy = spy(messageInfoLogger);
        doNothing().when(loggerSpy).log(any(Logger.class), eq(exchange), eq("req-out"));

        loggerSpy.logReqOut(exchange);

        verify(loggerSpy).log(any(Logger.class), eq(exchange), eq("req-out"));
    }

    @Test
    void testLogRespIn() {
        MessageInfoLogger loggerSpy = spy(messageInfoLogger);
        doNothing().when(loggerSpy).log(any(Logger.class), eq(exchange), eq("resp-in"));

        loggerSpy.logRespIn(exchange);

        verify(loggerSpy).log(any(Logger.class), eq(exchange), eq("resp-in"));
    }

    @Test
    void testLogRespOut() {
        MessageInfoLogger loggerSpy = spy(messageInfoLogger);
        doNothing().when(loggerSpy).log(any(Logger.class), eq(exchange), eq("resp-out"));

        loggerSpy.logRespOut(exchange);

        verify(loggerSpy).log(any(Logger.class), eq(exchange), eq("resp-out"));
    }

    @Test
    void testLogError() {
        String stackTrace = "test-stack-trace";
        LogMessageExceptionType exception = new LogMessageExceptionType();
        exception.setStackTrace(stackTrace);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("error", exchange))
                .thenReturn(logEntry);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createMessageException(exchange, stackTrace))
                .thenReturn(exception);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-error", logEntry))
                .thenReturn("formatted-error-message");

        messageInfoLogger.logError(exchange, stackTrace);

        logEntryBuilderMock.verify(() -> LogEntryBuilder.createLogEntry("error", exchange));
        logEntryBuilderMock.verify(() -> LogEntryBuilder.createMessageException(exchange, stackTrace));
        logMessageFormatterMock.verify(() -> LogMessageFormatter.format("logEvent-error", logEntry));
        assertEquals(exception, logEntry.getMessageInfo().getException());
        assertTrue(extraInfo.containsKey("source"));
    }

    @Test
    void testLogErrorWithException() {
        String stackTrace = "test-stack-trace";
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("error", exchange))
                .thenThrow(new RuntimeException("Test exception"));

        // Should handle gracefully
        assertDoesNotThrow(() -> messageInfoLogger.logError(exchange, stackTrace));
    }

    @Test
    void testLogWithDebugEnabled() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("req-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-debug", logEntry))
                .thenReturn("formatted-debug-message");

        messageInfoLogger.log(mockLogger, exchange, "req-in");

        verify(mockLogger).isDebugEnabled();
        verify(mockLogger).debug("formatted-debug-message");
        verify(mockLogger, never()).isInfoEnabled();
        assertEquals("test-body", logEntry.getPayload());
    }

    @Test
    void testLogWithInfoEnabled() {
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("req-out", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-info-message");

        messageInfoLogger.log(mockLogger, exchange, "req-out");

        verify(mockLogger).isDebugEnabled();
        verify(mockLogger).isInfoEnabled();
        verify(mockLogger).info("formatted-info-message");
        assertNull(logEntry.getPayload()); // Payload not set in info mode
    }

    @Test
    void testLogWithLoggingDisabled() {
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(mockLogger.isInfoEnabled()).thenReturn(false);

        messageInfoLogger.log(mockLogger, exchange, "req-in");

        verify(mockLogger).isDebugEnabled();
        verify(mockLogger).isInfoEnabled();
        verify(mockLogger, never()).debug(anyString());
        verify(mockLogger, never()).info(anyString());
        logEntryBuilderMock.verifyNoInteractions();
    }

    @Test
    void testLogWithException() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry(anyString(), any(Exchange.class)))
                .thenThrow(new RuntimeException("Test exception"));

        messageInfoLogger.log(mockLogger, exchange, "req-in");

        verify(mockLogger).error(eq("Failed log message: {}"), eq("req-in"), any(RuntimeException.class));
    }

    @Test
    void testLogRespInWithErrorResponse() {
        String soapBody = "<soap:Fault>test</soap:Fault>";
        SoapFaultInfo faultInfo = new SoapFaultInfo("soap:Server", "Error message", "Detail");
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(500);
        when(message.getBody(String.class)).thenReturn(soapBody);
        when(soapFaultExtractor.extractSoapFault(soapBody)).thenReturn(faultInfo);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-in");

        assertEquals(MessageInfoLogger.class.getName(), extraInfo.get("source")); // Verify basic extraInfo works
        verify(soapFaultExtractor).extractSoapFault(soapBody);
        assertEquals("soap:Server", extraInfo.get("faultCode"));
        assertEquals("Error message", extraInfo.get("faultString"));
        assertEquals("Detail", extraInfo.get("faultDetail"));
    }

    @Test
    void testLogRespInWithErrorResponseAndPartialFaultInfo() {
        String soapBody = "<soap:Fault>test</soap:Fault>";
        SoapFaultInfo faultInfo = new SoapFaultInfo("soap:Client", null, null);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(500);
        when(message.getBody(String.class)).thenReturn(soapBody);
        when(soapFaultExtractor.extractSoapFault(soapBody)).thenReturn(faultInfo);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-in");

        verify(soapFaultExtractor).extractSoapFault(soapBody);
        assertEquals("soap:Client", extraInfo.get("faultCode"));
        assertFalse(extraInfo.containsKey("faultString"));
        assertFalse(extraInfo.containsKey("faultDetail"));
    }

    @Test
    void testLogRespInWithErrorResponseButNoFaultInfo() {
        String soapBody = "<soap:Fault>test</soap:Fault>";
        SoapFaultInfo faultInfo = new SoapFaultInfo(null, null, null);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(500);
        when(message.getBody(String.class)).thenReturn(soapBody);
        when(soapFaultExtractor.extractSoapFault(soapBody)).thenReturn(faultInfo);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-in");

        verify(soapFaultExtractor).extractSoapFault(soapBody);
        assertFalse(extraInfo.containsKey("faultCode"));
        assertFalse(extraInfo.containsKey("faultString"));
        assertFalse(extraInfo.containsKey("faultDetail"));
    }

    @Test
    void testLogRespInWithNon500Response() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(200);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-in");

        verify(soapFaultExtractor, never()).extractSoapFault(anyString());
        assertFalse(extraInfo.containsKey("faultCode"));
    }

    @Test
    void testLogRespInWithNullResponseCode() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(null);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-in");

        verify(soapFaultExtractor, never()).extractSoapFault(anyString());
        assertFalse(extraInfo.containsKey("faultCode"));
    }

    @Test
    void testLogRespOutDoesNotExtractSoapFault() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        lenient().when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(500);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("resp-out", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "resp-out");

        verify(soapFaultExtractor, never()).extractSoapFault(anyString());
        assertFalse(extraInfo.containsKey("faultCode"));
    }

    @Test
    void testLogReqInDoesNotExtractSoapFault() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        lenient().when(message.getHeader(Exchange.HTTP_RESPONSE_CODE)).thenReturn(500);
        logEntryBuilderMock.when(() -> LogEntryBuilder.createLogEntry("req-in", exchange))
                .thenReturn(logEntry);
        logMessageFormatterMock.when(() -> LogMessageFormatter.format("logEvent-info", logEntry))
                .thenReturn("formatted-message");

        messageInfoLogger.log(mockLogger, exchange, "req-in");

        verify(soapFaultExtractor, never()).extractSoapFault(anyString());
        assertFalse(extraInfo.containsKey("faultCode"));
    }
}
