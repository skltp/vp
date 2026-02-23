package se.skl.tp.vp.logging.old;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.skl.tp.vp.constants.VPExchangeProperties;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LogExtraInfoBuilder with focus on addHttpForwardHeaders and addErrorInfo methods.
 */
class LogExtraInfoBuilderTest {

    private Exchange exchange;

    @BeforeEach
    void setUp() {
        exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
    }

    @Test
    void testAddHttpForwardHeaders_AllHeadersPresent() {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, "https");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, "example.com");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, "8443");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("https", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO should be added to extraInfo");
        assertEquals("example.com", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST should be added to extraInfo");
        assertEquals("8443", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT should be added to extraInfo");

        // Verify properties are removed after logging
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO property should be removed from exchange");
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST property should be removed from exchange");
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT property should be removed from exchange");
    }

    @Test
    void testAddHttpForwardHeaders_OnlyProtoPresent() {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, "http");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("http", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO should be added to extraInfo");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT should not be in extraInfo when not set");
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO property should be removed from exchange");
    }

    @Test
    void testAddHttpForwardHeaders_OnlyHostPresent() {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, "test.example.com");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO should not be in extraInfo when not set");
        assertEquals("test.example.com", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST should be added to extraInfo");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT should not be in extraInfo when not set");
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST property should be removed from exchange");
    }

    @Test
    void testAddHttpForwardHeaders_OnlyPortPresent() {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, "9090");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST should not be in extraInfo when not set");
        assertEquals("9090", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT should be added to extraInfo");
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT property should be removed from exchange");
    }

    @Test
    void testAddHttpForwardHeaders_NoHeadersPresent() {
        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO),
                "VP_X_FORWARDED_PROTO should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_HOST),
                "VP_X_FORWARDED_HOST should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.VP_X_FORWARDED_PORT),
                "VP_X_FORWARDED_PORT should not be in extraInfo when not set");
    }

    @ParameterizedTest
    @CsvSource({
            "https, localhost, 443",
            "http, api.test.com, 80",
            "https, secure.domain.org, 8443"
    })
    void testAddHttpForwardHeaders_VariousCombinations(String proto, String host, String port) {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, proto);
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, host);
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, port);

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals(proto, extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO));
        assertEquals(host, extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_HOST));
        assertEquals(port, extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PORT));

        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT));
    }

    @Test
    void testAddErrorInfo_WithExceptionAndAllErrorProperties() {
        Exception testException = new RuntimeException("Test error message");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP001");
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, "500");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals("Test error message", extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should contain exception message");
        assertTrue(extraInfo.get(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION)
                        .contains("RuntimeException"),
                "SESSION_ERROR_TECHNICAL_DESCRIPTION should contain exception class name");
        assertEquals("VP001", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be set");
        assertEquals("500", extraInfo.get(VPExchangeProperties.SESSION_HTML_STATUS),
                "SESSION_HTML_STATUS should be set");
    }

    @Test
    void testAddErrorInfo_WithExceptionButNoMessage() {
        Exception testException = new RuntimeException(); // No message
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP002");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals(LogExtraInfoBuilder.DEFAULT_ERROR_DESCRIPTION,
                extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should use default when exception has no message");
        assertTrue(extraInfo.get(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION)
                        .contains("RuntimeException"),
                "SESSION_ERROR_TECHNICAL_DESCRIPTION should contain exception class name");
        assertEquals("VP002", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be set");
    }

    @Test
    void testAddErrorInfo_WithoutException() {
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP003");
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, "404");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals(LogExtraInfoBuilder.DEFAULT_ERROR_DESCRIPTION,
                extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should use default when no exception");
        assertEquals("", extraInfo.get(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION),
                "SESSION_ERROR_TECHNICAL_DESCRIPTION should be empty when no exception");
        assertEquals("VP003", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be set");
        assertEquals("404", extraInfo.get(VPExchangeProperties.SESSION_HTML_STATUS),
                "SESSION_HTML_STATUS should be set");
    }

    @Test
    void testAddErrorInfo_WithNullErrorCode() {
        Exception testException = new IllegalArgumentException("Invalid argument");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        // SESSION_ERROR_CODE is not set (null)
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, "400");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals("Invalid argument", extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should contain exception message");
        assertEquals("", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be empty string when null");
        assertEquals("400", extraInfo.get(VPExchangeProperties.SESSION_HTML_STATUS),
                "SESSION_HTML_STATUS should be set");
    }

    @Test
    void testAddErrorInfo_WithNullHtmlStatus() {
        Exception testException = new IllegalStateException("Invalid state");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP004");
        // SESSION_HTML_STATUS is not set (null)

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals("Invalid state", extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should contain exception message");
        assertEquals("VP004", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be set");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_HTML_STATUS),
                "SESSION_HTML_STATUS should not be in extraInfo when null");
    }

    @Test
    void testAddErrorInfo_WithEmptyHtmlStatus() {
        Exception testException = new Exception("Test exception");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP005");
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, ""); // Empty string

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should be 'true'");
        assertEquals("VP005", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should be set");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_HTML_STATUS),
                "SESSION_HTML_STATUS should not be in extraInfo when empty");
    }

    @Test
    void testAddErrorInfo_NotCalledWhenNoError() {
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.FALSE);
        Exception testException = new RuntimeException("Should not be logged");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should not be in extraInfo when SESSION_ERROR is false");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should not be in extraInfo when SESSION_ERROR is false");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION),
                "SESSION_ERROR_TECHNICAL_DESCRIPTION should not be in extraInfo when SESSION_ERROR is false");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR_CODE),
                "SESSION_ERROR_CODE should not be in extraInfo when SESSION_ERROR is false");
    }

    @Test
    void testAddErrorInfo_NotCalledWhenErrorPropertyNull() {
        // SESSION_ERROR is not set (null)
        Exception testException = new RuntimeException("Should not be logged");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR),
                "SESSION_ERROR should not be in extraInfo when SESSION_ERROR property is null");
        assertFalse(extraInfo.containsKey(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "SESSION_ERROR_DESCRIPTION should not be in extraInfo when SESSION_ERROR property is null");
    }

    @Test
    void testCreateExtraInfo_WithBothForwardHeadersAndError() {
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, "https");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, "error.example.com");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, "443");

        Exception testException = new RuntimeException("Combined test error");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP006");
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, "503");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("https", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PROTO));
        assertEquals("error.example.com", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_HOST));
        assertEquals("443", extraInfo.get(LogExtraInfoBuilder.VP_X_FORWARDED_PORT));
        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR));
        assertEquals("Combined test error", extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION));
        assertTrue(extraInfo.get(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION)
                .contains("RuntimeException"));
        assertEquals("VP006", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE));
        assertEquals("503", extraInfo.get(VPExchangeProperties.SESSION_HTML_STATUS));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT));
    }

    @Test
    void testCreateExtraInfo_WithNestedExceptionMessage() {
        Exception rootCause = new IllegalStateException("Root cause");
        Exception wrappedException = new RuntimeException("Wrapped exception", rootCause);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, wrappedException);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP007");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("true", extraInfo.get(VPExchangeProperties.SESSION_ERROR));
        assertEquals("Wrapped exception", extraInfo.get(VPExchangeProperties.SESSION_ERROR_DESCRIPTION),
                "Should use the outer exception's message");
        String technicalDesc = extraInfo.get(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION);
        assertTrue(technicalDesc.contains("RuntimeException"),
                "Technical description should contain outer exception type");
        assertEquals("VP007", extraInfo.get(VPExchangeProperties.SESSION_ERROR_CODE));
    }

    @Test
    void testAddSoapFaultInfo_WithAllSoapFaultFields() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:Server");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, "Internal server error");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, "Detailed error information");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("soap:Server", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should be added to extraInfo");
        assertEquals("Internal server error", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should be added to extraInfo");
        assertEquals("Detailed error information", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should be added to extraInfo");
    }

    @Test
    void testAddSoapFaultInfo_WithOnlyFaultCode() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:Client");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertEquals("soap:Client", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should be added to extraInfo");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should not be in extraInfo when not set");
    }

    @Test
    void testAddSoapFaultInfo_WithOnlyFaultString() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, "Service unavailable");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should not be in extraInfo when not set");
        assertEquals("Service unavailable", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should be added to extraInfo");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should not be in extraInfo when not set");
    }

    @Test
    void testAddSoapFaultInfo_WithOnlyFaultDetail() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, "Connection timeout");

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should not be in extraInfo when not set");
        assertEquals("Connection timeout", extraInfo.get(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should be added to extraInfo");
    }

    @Test
    void testAddSoapFaultInfo_NoSoapFaultFieldsPresent() {
        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should not be in extraInfo when not set");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should not be in extraInfo when not set");
    }

    @Test
    void testAddSoapFaultInfo_WithNullValues() {
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, null);
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, null);
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, null);

        Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);

        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_CODE),
                "SOAP_FAULT_CODE should not be in extraInfo when null");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_STRING),
                "SOAP_FAULT_STRING should not be in extraInfo when null");
        assertFalse(extraInfo.containsKey(LogExtraInfoBuilder.SOAP_FAULT_DETAIL),
                "SOAP_FAULT_DETAIL should not be in extraInfo when null");
    }
}