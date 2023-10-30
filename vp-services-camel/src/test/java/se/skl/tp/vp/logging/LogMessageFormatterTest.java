package se.skl.tp.vp.logging;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.logentry.*;
import se.skl.tp.vp.util.JunitUtil;
import se.skl.tp.vp.util.TestLogAppender;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.wildfly.common.Assert.assertNotNull;
import static se.skl.tp.vp.util.TestLogAppender.assertLogMessage;

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
    private MessageInfoLogger messageInfoLogger;
    private String exceptionMessage;
    private String exceptionClass;
    private String businessCorrId;

    @BeforeEach
    public void beforeEach() {
        infoLogger = mock(Logger.class);
        debugLogger = mock(Logger.class);
        exchange = mock(Exchange.class);
        messageInfoLogger = new MessageInfoLogger();
        logEventName = "logEvent-test";
        payload = "<Payload/>";
        stackTrace = "<Stacktrace/>";
        exceptionMessage = "<ExceptionMessage/>";
        exceptionClass = "<ExceptionClass/>";
        messageType = "testObjectLogger";
        Message in = mock(Message.class);
        CamelContext ctx = mock(CamelContext.class);
        businessCorrId = UUID.randomUUID().toString();


        Mockito.when(exchange.getProperty(VPExchangeProperties.EXCHANGE_CREATED, Date.class)).thenReturn(new Date());
        Mockito.when(exchange.getProperty(anyString(), eq(String.class))).thenReturn("mockedString");
        Mockito.when(exchange.getMessage()).thenReturn(in);
        Mockito.when(exchange.getIn()).thenReturn(in);
        Mockito.when(exchange.getContext()).thenReturn(ctx);
        Mockito.when(infoLogger.isDebugEnabled()).thenReturn(false);
        Mockito.when(infoLogger.isInfoEnabled()).thenReturn(true);
        Mockito.when(debugLogger.isInfoEnabled()).thenReturn(false);
        Mockito.when(debugLogger.isDebugEnabled()).thenReturn(true);

    }

    private LogEntry getLogEntry() {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessageInfo(new LogMessageType());
        logEntry.setMetadataInfo(new LogMetadataInfoType());
        logEntry.setExtraInfo(new HashMap<>());
        logEntry.setRuntimeInfo(new LogRuntimeInfoType());
        setLogEntryRuntimeInfo(logEntry,"unitTest", "testMessage", businessCorrId);
        return logEntry;
    }

    static private void setLogEntryException(LogEntry logEntry,
                                             String stackTrace,
                                             String exceptionMessage,
                                             String exceptionClass) {
        LogMessageExceptionType logMessageException = new LogMessageExceptionType();
        logMessageException.setStackTrace(stackTrace);
        logMessageException.setExceptionMessage(exceptionMessage);
        logMessageException.setExceptionClass(exceptionClass);
        logEntry.getMessageInfo().setException(logMessageException);
    }

    static private void setLogEntryRuntimeInfo(LogEntry logEntry, String componentId, String messageId, String businessCorrelationId) {
        LogRuntimeInfoType rti = logEntry.getRuntimeInfo();
        rti.setMessageId(messageId);
        rti.setComponentId(componentId);
        rti.setBusinessCorrelationId(businessCorrelationId);
    }

    static private void setLogEntryExtraInfo(LogEntry logEntry, String ... extra) {
        String key = null;
        for (String e: extra) {
            if (key == null ) {
                key = e;
            } else {
                logEntry.getExtraInfo().put(key, e);
                key = null;
            }
        }
    }

    @Test
    public void testFormatWithObject() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        LogMessageFormatter.format(logEventName, getLogEntry(), messageMap);

        assertNotNull(messageMap);
        assertEquals(logEventName, messageMap.get("logEventType"));
        assertNull("payload should be null", messageMap.get("payload"));

    }

    @Test
    public void testFormatWithString() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        LogEntry logEntry = getLogEntry();
        setLogEntryException(logEntry, stackTrace, exceptionMessage, exceptionClass);

        setLogEntryExtraInfo(logEntry,
                "testFormat", "withString",
                "extraInfo", "myInfo");

        // MUT
        String logString = LogMessageFormatter.format(logEventName, logEntry);

        logEntry.setPayload(payload);
        // MUT
        String logStringWithPayload = LogMessageFormatter.format(logEventName, logEntry);

        assertNotNull(logString);
        assertNotNull(logStringWithPayload);


        JunitUtil.assertMatchRegexGroup(logString, "BusinessCorrelationId=(.*)", businessCorrId, 1);
        JunitUtil.assertMatchRegexGroup(logString, "MessageId=(.*)", "testMessage", 1);
        JunitUtil.assertMatchRegexGroup(logString, "LogMessage=(.*)", "null", 1);
        JunitUtil.assertMatchRegexGroup(logString, "Stacktrace=(.*)", stackTrace, 1);
        JunitUtil.assertMatchRegexGroup(logString, "-testFormat=(.*)", "withString", 1);
        JunitUtil.assertMatchRegexGroup(logString, "-extraInfo=(.*)", "myInfo", 1);


        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "LogMessage=(.*)", "null", 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "Stacktrace=(.*)", stackTrace, 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "Payload=(.*)", payload, 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "-testFormat=(.*)", "withString", 1);
        JunitUtil.assertMatchRegexGroup(logStringWithPayload, "-extraInfo=(.*)", "myInfo", 1);

        logEntry.setExtraInfo(null);
        String logStringWithoutExtra = LogMessageFormatter.format(logEventName, logEntry);
        JunitUtil.assertMatchRegexGroup(logStringWithoutExtra, "ExtraInfo=(.*)", "", 1);

    }

    @Test
    public void testLogAndObjLog() throws Exception {
        messageInfoLogger.log(debugLogger, exchange, messageType);
        messageInfoLogger.objLog(debugLogger, exchange, messageType);
        Mockito.verify(debugLogger, times(1)).debug(anyString());
        Mockito.verify(debugLogger, times(1)).debug(isA(ObjectMessage.class));
        Mockito.verify(debugLogger, times(0)).info(anyString());
        Mockito.verify(debugLogger, times(0)).error(anyString(), nullable(Object.class), nullable(Object.class));
        messageInfoLogger.log(infoLogger, exchange, messageType);
        messageInfoLogger.objLog(infoLogger, exchange, messageType);
        Mockito.verify(infoLogger, times(1)).info(anyString());
        Mockito.verify(infoLogger, times(1)).info(isA(ObjectMessage.class));
        Mockito.verify(infoLogger, times(0)).debug(anyString());
        Mockito.verify(infoLogger, times(0)).error(anyString(), nullable(Object.class), nullable(Object.class));
        messageInfoLogger.log(infoLogger, null, messageType);
        messageInfoLogger.objLog(infoLogger, null, messageType);
        Mockito.verify(infoLogger, times(2)).error(anyString(), nullable(Object.class), nullable(Object.class));
        Mockito.verify(infoLogger, times(1)).info(anyString()); // from previous call
        Mockito.verify(infoLogger, times(1)).info(isA(ObjectMessage.class)); // from previous call
        Mockito.verify(infoLogger, times(0)).debug(anyString());
    }

    @Test
    public void testLogError() {
        messageInfoLogger.logError(exchange, stackTrace);
        org.apache.logging.log4j.message.Message respOutLogMsg = TestLogAppender.getEventMessageObject(MessageInfoLogger.REQ_ERROR, 0);
        assertNotNull(respOutLogMsg);
        String formatted = respOutLogMsg.getFormattedMessage();
        assertLogMessage("se.skl.tp.vp.logging.error",
                "mockedString",
                "mockedString",
                "error",
                "mockedString",
                "mockedString");

        messageInfoLogger.logError(null, stackTrace);
        org.apache.logging.log4j.message.Message respOutLogMsgE = TestLogAppender.getEventMessageObject(MessageInfoLogger.REQ_ERROR, 0);
        assertNotNull(respOutLogMsg);
        String formattedE = respOutLogMsg.getFormattedMessage();
        assertLogMessage("se.skl.tp.vp.logging.error",
                "mockedString",
                "mockedString",
                "error",
                "mockedString",
                "mockedString");
    }

    @Test
    public void testObjectMessageLog_() throws Exception {
        HashMap<String, Object> messageMap = new HashMap<>();
        LogEntry logEntry = getLogEntry();
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

    @Test
    public void testCreateWsdlNamespaceReturnsNullOnNullArguments() throws Exception {
        Mockito.when(exchange.getProperty(VPExchangeProperties.RIV_VERSION, String.class)).thenReturn(null);
        LogEntry le = LogEntryBuilder.createLogEntry("no-riv", exchange, false);
        assertNull(VPExchangeProperties.RIV_VERSION + " should be null", le.getExtraInfo().get(VPExchangeProperties.RIV_VERSION));
    }

    @Test public void testObjLogErrorWithNullExchangeLogsError() throws Exception {

        messageInfoLogger.objLogError(null, stackTrace);
        org.apache.logging.log4j.message.Message respOutLogMsg = TestLogAppender.getEventMessageObject(MessageInfoLogger.REQ_ERROR, 0);
        assertNotNull(respOutLogMsg);

    }

}
