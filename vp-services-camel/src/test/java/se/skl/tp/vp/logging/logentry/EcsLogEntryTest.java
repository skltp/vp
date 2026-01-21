package se.skl.tp.vp.logging.logentry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("HttpUrlsUsage") // Some test data use http URLs for simplicity
class EcsLogEntryTest {

    private Exchange exchange;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
    }

    @Test
    void testBuilderCreatesRequestLogEntry() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        EcsLogEntry entry = builder.build();

        assertEquals("req-in", entry.get(EcsFields.EVENT_ACTION));
        assertEquals(EcsLogEntry.EVENT_KIND_VALUE, entry.get(EcsFields.EVENT_KIND));
        assertEquals(EcsLogEntry.EVENT_CATEGORY_VALUE, entry.get(EcsFields.EVENT_CATEGORY));
        assertEquals(EcsLogEntry.EVENT_TYPE_VALUE_REQ, entry.get(EcsFields.EVENT_TYPE));
        assertEquals(EcsLogEntry.EVENT_MODULE_VALUE, entry.get(EcsFields.EVENT_MODULE));
    }

    @Test
    void testBuilderCreatesResponseLogEntry() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        EcsLogEntry entry = builder.build();

        assertEquals("resp-out", entry.get(EcsFields.EVENT_ACTION));
        assertEquals(EcsLogEntry.EVENT_TYPE_VALUE_RESP, entry.get(EcsFields.EVENT_TYPE));
    }

    @Test
    void testWithPayloadForRequest() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        EcsLogEntry entry = builder.build().withPayload("<soap>request</soap>");

        assertEquals("<soap>request</soap>", entry.get(EcsFields.HTTP_REQUEST_BODY_CONTENT));
        assertNull(entry.get(EcsFields.HTTP_RESPONSE_BODY_CONTENT));
    }

    @Test
    void testWithPayloadForResponse() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        EcsLogEntry entry = builder.build().withPayload("<soap>response</soap>");

        assertEquals("<soap>response</soap>", entry.get(EcsFields.HTTP_RESPONSE_BODY_CONTENT));
        assertNull(entry.get(EcsFields.HTTP_REQUEST_BODY_CONTENT));
    }

    @Test
    void testWithPayloadNullDoesNotAddField() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        EcsLogEntry entry = builder.build().withPayload(null);

        assertNull(entry.get(EcsFields.HTTP_REQUEST_BODY_CONTENT));
    }

    @Test
    void testFromExchangeWithNullExchange() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(null);
        EcsLogEntry entry = builder.build();

        // Should not throw exception and should only have basic fields
        assertNotNull(entry);
        assertEquals("req-in", entry.get(EcsFields.EVENT_ACTION));
    }

    @Test
    void testFromExchangeBasicFields() {
        exchange.setProperty(VPExchangeProperties.HTTP_URL_IN, "http://test.example.com/service");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "test-correlation-id");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        // SERVICE_NAME and LABEL_ROUTE may be null or have default values depending on context
        assertEquals("http://test.example.com/service", entry.get(EcsFields.URL_FULL));
        assertEquals("POST", entry.get(EcsFields.HTTP_REQUEST_METHOD));
        assertEquals("200", entry.get(EcsFields.HTTP_RESPONSE_STATUS_CODE));
        assertEquals("test-correlation-id", entry.get(EcsFields.TRACE_ID));
        assertNotNull(entry.get(EcsFields.TRANSACTION_ID));
        assertNotNull(entry.get(EcsFields.EVENT_ID));
    }

    @Test
    void testFromExchangeWithSpanIds() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, "span-a");
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, "span-b");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("span-a", entry.get(EcsFields.LABELS + "parent.id"));
        assertEquals("span-b", entry.get(EcsFields.SPAN_ID));
    }

    @Test
    void testFromExchangeWithOnlySpanA() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, "span-a");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("span-a", entry.get(EcsFields.SPAN_ID));
        assertNull(entry.get(EcsFields.LABELS + "parent.id"));
    }

    @Test
    void testFromExchangeWithSenderAndReceiverIds() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SENDER_ID, "sender-123");
        exchange.setProperty(VPExchangeProperties.RECEIVER_ID, "receiver-456");
        exchange.setProperty(VPExchangeProperties.SENDER_IP_ADRESS, "192.168.1.1");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("sender-123", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SENDER_ID));
        assertEquals("receiver-456", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_RECEIVER_ID));
        assertEquals("192.168.1.1", entry.get(EcsFields.SOURCE_ADDRESS));
        assertEquals("192.168.1.1", entry.get(EcsFields.SOURCE_IP));
    }

    @Test
    void testFromExchangeWithServiceContractAndRivVersion() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, "urn:riv:test:ServiceResponder:1");
        exchange.setProperty(VPExchangeProperties.RIV_VERSION, "RIVTABP21");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("urn:riv:test:ServiceResponder:1", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SERVICECONTRACT_NAMESPACE));
        assertEquals("RIVTABP21", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_RIV_VERSION));
        assertEquals("urn:riv:test:Service:1:RIVTABP21", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_WSDL_NAMESPACE));
    }

    @Test
    void testFromExchangeWithDestination() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.VAGVAL, "http://backend.example.com:8080/service");
        exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, "backend.example.com:8080");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("http://backend.example.com:8080/service", entry.get(EcsFields.URL_ORIGINAL));
        assertEquals("backend.example.com:8080", entry.get(EcsFields.DESTINATION_ADDRESS));
        assertEquals("backend.example.com", entry.get(EcsFields.DESTINATION_DOMAIN));
        assertEquals("8080", entry.get(EcsFields.DESTINATION_PORT));
    }

    @Test
    void testFromExchangeWithDestinationWithoutPort() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, "backend.example.com");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("backend.example.com", entry.get(EcsFields.DESTINATION_ADDRESS));
        assertEquals("backend.example.com", entry.get(EcsFields.DESTINATION_DOMAIN));
        assertNull(entry.get(EcsFields.DESTINATION_PORT));
    }

    @Test
    void testFromExchangeWithHeaders() throws Exception {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.getIn().setHeader("Content-Type", "text/xml");
        exchange.getIn().setHeader("X-Custom-Header", "custom-value");
        exchange.getIn().setHeader("content-length", "1234");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String headersJson = entry.get(EcsFields.HTTP_REQUEST_HEADERS);
        assertNotNull(headersJson);

        Map<String, String> headers = objectMapper.readValue(headersJson, new TypeReference<>() {});
        assertEquals("text/xml", headers.get("Content-Type"));
        assertEquals("custom-value", headers.get("X-Custom-Header"));
        assertEquals("1234", entry.get(EcsFields.HTTP_REQUEST_BODY_BYTES));
    }

    @Test
    void testFromExchangeWithResponseHeaders() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.getIn().setHeader("Content-Type", "text/xml");
        exchange.getIn().setHeader("content-length", "5678");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String headersJson = entry.get(EcsFields.HTTP_RESPONSE_HEADERS);
        assertNotNull(headersJson);
        assertEquals("5678", entry.get(EcsFields.HTTP_RESPONSE_BODY_BYTES));
    }

    @Test
    void testFromExchangeWithOriginalConsumerIds() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "out-consumer-123");
        exchange.setProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "in-consumer-456");
        exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ACTING_ON_BEHALF_OF_HSA_ID, "acting-789");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("out-consumer-123", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
        assertEquals("in-consumer-456", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
        assertEquals("acting-789", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_ACTING_ON_BEHALF_OF_HSA_ID));
    }

    @Test
    void testFromExchangeWithTraceProperties() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.VAGVAL_TRACE, "vagval-trace-info");
        exchange.setProperty(VPExchangeProperties.ANROPSBEHORIGHET_TRACE, "behorighet-trace-info");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("vagval-trace-info", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VAGVAL_TRACE));
        assertEquals("behorighet-trace-info", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_ANROPSBEHORIGHET_TRACE));
    }

    @Test
    void testFromExchangeWithSoapFault() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_CODE, "soap:Server");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_STRING, "Internal server error");
        exchange.setProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, "Detailed error information");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("soap:Server", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SOAP_FAULT_CODE));
        assertEquals("Internal server error", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SOAP_FAULT_STRING));
        assertEquals("Detailed error information", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SOAP_FAULT_DETAIL));
    }

    @Test
    void testFromExchangeWithError() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, true);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP001");
        exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, "500");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, new RuntimeException("Test error message"));

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("true", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR));
        assertEquals("Test error message", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR_DESCRIPTION));
        assertTrue(entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR_TECHNICAL_DESCRIPTION).contains("RuntimeException"));
        assertEquals("VP001", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR_CODE));
        assertEquals("500", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_HTML_STATUS));
    }

    @Test
    void testFromExchangeWithErrorButNoException() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR, true);
        exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, "VP001");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("true", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR));
        assertEquals(EcsLogEntry.Builder.DEFAULT_ERROR_DESCRIPTION,
            entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SESSION_ERROR_DESCRIPTION));
    }

    @Test
    void testWithException() {
        RuntimeException exception = new RuntimeException("Test exception");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("error");
        builder.fromExchange(exchange);
        builder.withException(exchange, "Stack trace here");
        EcsLogEntry entry = builder.build();

        assertEquals("java.lang.RuntimeException", entry.get(EcsFields.ERROR_TYPE));
        assertEquals("Test exception", entry.get(EcsFields.ERROR_MESSAGE));
        assertEquals("Stack trace here", entry.get(EcsFields.ERROR_STACK_TRACE));
    }

    @Test
    void testWithExceptionNoException() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("error");
        builder.fromExchange(exchange);
        builder.withException(exchange, "Stack trace here");
        EcsLogEntry entry = builder.build();

        assertNull(entry.get(EcsFields.ERROR_TYPE));
        assertNull(entry.get(EcsFields.ERROR_MESSAGE));
        assertNull(entry.get(EcsFields.ERROR_STACK_TRACE));
    }

    @Test
    void testWithHttpForwardHeaders() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, "https");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, "proxy.example.com");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, "443");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        builder.withHttpForwardHeaders(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("https", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_PROTO));
        assertEquals("proxy.example.com", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_HOST));
        assertEquals("443", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_PORT));

        // Verify properties are removed
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST));
        assertNull(exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT));
    }

    @Test
    void testWithHttpForwardHeadersPartial() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, "https");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        builder.withHttpForwardHeaders(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("https", entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_PROTO));
        assertNull(entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_HOST));
        assertNull(entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_VP_X_FORWARDED_PORT));
    }

    @Test
    void testFilterHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "text/xml");
        headers.put("X-Forwarded-Tls-Client-Cert", "SECRET_CERT");
        headers.put("x-vp-auth-cert", "SECRET_AUTH");
        headers.put("X-FK-AUTH-CERT", "SECRET_FK");
        headers.put("Custom-Header", "custom-value");

        Map<String, String> result = new HashMap<>();
        EcsLogEntry.Builder.filterHeaders(headers, result);

        assertEquals("text/xml", result.get("Content-Type"));
        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("X-Forwarded-Tls-Client-Cert"));
        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("x-vp-auth-cert"));
        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("X-FK-AUTH-CERT"));
        assertEquals("custom-value", result.get("Custom-Header"));
    }

    @Test
    void testFilterHeadersCaseInsensitive() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-forwarded-tls-client-cert", "SECRET");
        headers.put("X-VP-AUTH-CERT", "SECRET");
        headers.put("x-fk-Auth-Cert", "SECRET");

        Map<String, String> result = new HashMap<>();
        EcsLogEntry.Builder.filterHeaders(headers, result);

        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("x-forwarded-tls-client-cert"));
        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("X-VP-AUTH-CERT"));
        assertEquals(EcsLogEntry.Builder.FILTERED_TEXT, result.get("x-fk-Auth-Cert"));
    }

    @Test
    void testPutDataWithNullValueDoesNotAdd() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.putData("test.field", null);
        EcsLogEntry entry = builder.build();

        assertNull(entry.get("test.field"));
    }

    @Test
    void testPutDataWithValidValue() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.putData("test.field", "test-value");
        EcsLogEntry entry = builder.build();

        assertEquals("test-value", entry.get("test.field"));
    }

    @Test
    void testEventDurationForResponse() {
        Date created = new Date(System.currentTimeMillis() - 100); // 100ms ago
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, created);
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String duration = entry.get(EcsFields.EVENT_DURATION);
        assertNotNull(duration);
        // Duration should be in nanoseconds, at least 100ms = 100,000,000 ns
        long durationNs = Long.parseLong(duration);
        assertTrue(durationNs >= 100000000L, "Duration should be at least 100ms in nanoseconds");
    }

    @Test
    void testEventDurationNotSetForRequest() {
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertNull(entry.get(EcsFields.EVENT_DURATION));
    }

    @Test
    void testEventDurationFromProducerResponseTime() {
        exchange.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, "span-a");
        exchange.setProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, "span-b");
        exchange.getIn().setHeader(HttpHeaders.X_SKLTP_PRODUCER_RESPONSETIME, "250");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("250000000", entry.get(EcsFields.EVENT_DURATION)); // 250ms converted to ns
    }

    @Test
    void testCreateWsdlNamespace() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
            "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");
        exchange.setProperty(VPExchangeProperties.RIV_VERSION, "RIVTABP21");

        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String wsdlNamespace = entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_WSDL_NAMESPACE);
        assertEquals("urn:riv:clinicalprocess:activity:actions:GetActivities:1:RIVTABP21", wsdlNamespace);
    }

    @Test
    void testCreateWsdlNamespaceWithNullServiceContract() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.RIV_VERSION, "RIVTABP21");

        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertNull(entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_WSDL_NAMESPACE));
    }

    @Test
    void testCreateWsdlNamespaceWithNullRivVersion() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");
        exchange.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
            "urn:riv:test:ServiceResponder:1");

        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertNull(entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_WSDL_NAMESPACE));
    }

    @Test
    void testCompleteFlowFromRequestToResponse() {
        // Simulate a complete request-response flow
        exchange.setProperty(VPExchangeProperties.HTTP_URL_IN, "http://test.example.com/service");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "flow-corr-id");
        exchange.setProperty(VPExchangeProperties.SENDER_ID, "sender-abc");
        exchange.setProperty(VPExchangeProperties.RECEIVER_ID, "receiver-xyz");
        exchange.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, "urn:riv:test:ServiceResponder:1");
        exchange.setProperty(VPExchangeProperties.RIV_VERSION, "RIVTABP21");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader("Content-Type", "text/xml");

        // Request log entry
        EcsLogEntry.Builder requestBuilder = new EcsLogEntry.Builder("req-in");
        requestBuilder.fromExchange(exchange);
        EcsLogEntry requestEntry = requestBuilder.build();

        assertEquals("req-in", requestEntry.get(EcsFields.EVENT_ACTION));
        assertEquals("flow-corr-id", requestEntry.get(EcsFields.TRACE_ID));
        assertEquals("sender-abc", requestEntry.get(EcsFields.LABELS + EcsLogEntry.LABEL_SENDER_ID));

        // Response log entry
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200");
        EcsLogEntry.Builder responseBuilder = new EcsLogEntry.Builder("resp-out");
        responseBuilder.fromExchange(exchange);
        EcsLogEntry responseEntry = responseBuilder.build();

        assertEquals("resp-out", responseEntry.get(EcsFields.EVENT_ACTION));
        assertEquals("flow-corr-id", responseEntry.get(EcsFields.TRACE_ID));
        assertEquals("200", responseEntry.get(EcsFields.HTTP_RESPONSE_STATUS_CODE));
        assertNotNull(responseEntry.get(EcsFields.EVENT_DURATION));
    }

    @Test
    void testBackwardCompatibilityFieldLogMessage() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        EcsLogEntry entry = builder.build();

        assertEquals("req-in", entry.get(EcsLogEntry.BACKWARD_COMPAT_LOG_MESSAGE));
        assertEquals(entry.get(EcsFields.EVENT_ACTION), entry.get(EcsLogEntry.BACKWARD_COMPAT_LOG_MESSAGE));
    }

    @Test
    void testBackwardCompatibilityFieldServiceImpl() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String route = entry.get(EcsFields.LABELS + EcsLogEntry.LABEL_ROUTE);
        assertEquals(route, entry.get(EcsLogEntry.BACKWARD_COMPAT_SERVICE_IMPL));
    }

    @Test
    void testBackwardCompatibilityFieldComponentId() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String serviceName = entry.get(EcsFields.SERVICE_NAME);
        assertEquals(serviceName, entry.get(EcsLogEntry.BACKWARD_COMPAT_COMPONENT_ID));
        assertEquals(entry.get(EcsFields.SERVICE_NAME), entry.get(EcsLogEntry.BACKWARD_COMPAT_COMPONENT_ID));
    }

    @Test
    void testBackwardCompatibilityFieldEndpoint() {
        exchange.setProperty(VPExchangeProperties.HTTP_URL_IN, "http://example.com/service");
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("http://example.com/service", entry.get(EcsLogEntry.BACKWARD_COMPAT_ENDPOINT));
        assertEquals(entry.get(EcsFields.URL_FULL), entry.get(EcsLogEntry.BACKWARD_COMPAT_ENDPOINT));
    }

    @Test
    void testBackwardCompatibilityFieldMessageId() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-id");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        String transactionId = entry.get(EcsFields.TRANSACTION_ID);
        assertNotNull(transactionId);
        assertEquals(transactionId, entry.get(EcsLogEntry.BACKWARD_COMPAT_MESSAGE_ID));
    }

    @Test
    void testBackwardCompatibilityFieldBusinessCorrelationId() {
        exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "business-corr-123");

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertEquals("business-corr-123", entry.get(EcsLogEntry.BACKWARD_COMPAT_BUSINESS_CORRELATION_ID));
        assertEquals(entry.get(EcsFields.TRACE_ID), entry.get(EcsLogEntry.BACKWARD_COMPAT_BUSINESS_CORRELATION_ID));
    }

    @Test
    void testBackwardCompatibilityFieldsAllPresent() throws Exception {
        // Create a route so that exchange.getFromRouteId() returns a value
        CamelContext camelContext = exchange.getContext();
        camelContext.addRoutes(new org.apache.camel.builder.RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test").routeId("test-route").to("mock:result");
            }
        });
        camelContext.start();

        // Create exchange from the route using ProducerTemplate
        exchange = camelContext.createProducerTemplate().request("direct:test", ex -> {
            ex.setProperty(VPExchangeProperties.EXCHANGE_CREATED, new Date());
            ex.setProperty(VPExchangeProperties.HTTP_URL_IN, "http://test.com/endpoint");
            ex.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "corr-456");
        });

        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("resp-out");
        builder.fromExchange(exchange);
        EcsLogEntry entry = builder.build();

        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_LOG_MESSAGE));
        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_SERVICE_IMPL));
        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_COMPONENT_ID));
        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_ENDPOINT));
        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_MESSAGE_ID));
        assertNotNull(entry.get(EcsLogEntry.BACKWARD_COMPAT_BUSINESS_CORRELATION_ID));
        assertEquals(entry.get(EcsFields.EVENT_ACTION), entry.get(EcsLogEntry.BACKWARD_COMPAT_LOG_MESSAGE));
        assertEquals(entry.get(EcsFields.SERVICE_NAME), entry.get(EcsLogEntry.BACKWARD_COMPAT_COMPONENT_ID));
        assertEquals(entry.get(EcsFields.URL_FULL), entry.get(EcsLogEntry.BACKWARD_COMPAT_ENDPOINT));
        assertEquals(entry.get(EcsFields.TRANSACTION_ID), entry.get(EcsLogEntry.BACKWARD_COMPAT_MESSAGE_ID));
        assertEquals(entry.get(EcsFields.TRACE_ID), entry.get(EcsLogEntry.BACKWARD_COMPAT_BUSINESS_CORRELATION_ID));

        camelContext.stop();
    }

    @Test
    void testBackwardCompatibilityFieldsWithNullValues() {
        EcsLogEntry.Builder builder = new EcsLogEntry.Builder("req-in");
        EcsLogEntry entry = builder.build();

        assertEquals("req-in", entry.get(EcsLogEntry.BACKWARD_COMPAT_LOG_MESSAGE));

        assertDoesNotThrow(() -> entry.get(EcsLogEntry.BACKWARD_COMPAT_SERVICE_IMPL));
        assertDoesNotThrow(() -> entry.get(EcsLogEntry.BACKWARD_COMPAT_COMPONENT_ID));
        assertDoesNotThrow(() -> entry.get(EcsLogEntry.BACKWARD_COMPAT_ENDPOINT));
    }
}
