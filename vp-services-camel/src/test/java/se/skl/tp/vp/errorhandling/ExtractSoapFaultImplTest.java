package se.skl.tp.vp.errorhandling;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import se.skl.tp.vp.constants.VPExchangeProperties;

import javax.xml.transform.TransformerConfigurationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("HttpUrlsUsage")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractSoapFaultImplTest {
  
  private final ExtractSoapFaultImpl extractSoapFault = new ExtractSoapFaultImpl();
  
  @Test
  void testExtractSimpleSoapFault() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Header/>
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Server</faultcode>
              <faultstring>Internal Server Error</faultstring>
              <detail></detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Server", result.faultCode());
    assertEquals("Internal Server Error", result.faultString());
    assertNotNull(result.detail());
  }

  @Test
  void testExtractSoapFaultWithStructuredDetail() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Header/>
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Client</faultcode>
              <faultstring>VP004 [NTjP] Det finns inget vägval i tjänsteadresseringskatalogen</faultstring>
              <detail>
                <detailString>No receiverId (logical address) found for serviceNamespace: urn:example, receiverId: 12345</detailString>
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Client", result.faultCode());
    assertEquals("VP004 [NTjP] Det finns inget vägval i tjänsteadresseringskatalogen", result.faultString());
    assertNotNull(result.detail());
    assertTrue(result.detail().contains("<detail>"));
    assertTrue(result.detail().contains("detailString"));
    assertTrue(result.detail().contains("No receiverId (logical address) found for serviceNamespace: urn:example, receiverId: 12345"));
  }

  @Test
  void testExtractSoapFaultWithComplexDetail() {
    String soapFault = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <soap:Fault>
              <faultcode>soap:Server</faultcode>
              <faultstring>VP009 [NTjP] Tekniskt fel vid anrop mot tjänsteproducent</faultstring>
              <detail>
                <errorDetails xmlns="http://example.org/errors">
                  <errorCode>TIMEOUT</errorCode>
                  <message>Connection timeout after 30000ms</message>
                  <timestamp>2026-01-14T12:00:00Z</timestamp>
                </errorDetails>
              </detail>
            </soap:Fault>
          </soap:Body>
        </soap:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soap:Server", result.faultCode());
    assertEquals("VP009 [NTjP] Tekniskt fel vid anrop mot tjänsteproducent", result.faultString());
    assertNotNull(result.detail());
    // Verify the detail contains the structured XML
    assertTrue(result.detail().contains("<detail>"));
    assertTrue(result.detail().contains("errorDetails"));
    assertTrue(result.detail().contains("TIMEOUT"));
    assertTrue(result.detail().contains("Connection timeout after 30000ms"));
    assertTrue(result.detail().contains("2026-01-14T12:00:00Z"));
  }

  @Test
  void testExtractSoapFaultWithEmptyDetail() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Client</faultcode>
              <faultstring>Invalid request</faultstring>
              <detail/>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Client", result.faultCode());
    assertEquals("Invalid request", result.faultString());
    assertNotNull(result.detail());
  }

  @Test
  void testExtractSoapFaultWithoutDetail() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Server</faultcode>
              <faultstring>Service unavailable</faultstring>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Server", result.faultCode());
    assertEquals("Service unavailable", result.faultString());
    assertNull(result.detail());
  }

  @Test
  void testExtractSoapFaultWithSpecialCharacters() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Client</faultcode>
              <faultstring>Invalid input: &lt;tag&gt; &amp; "quotes"</faultstring>
              <detail>
                <message>Error with &lt;xml&gt; and &amp; characters</message>
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Client", result.faultCode());
    assertEquals("Invalid input: <tag> & \"quotes\"", result.faultString());
    assertNotNull(result.detail());
    assertTrue(result.detail().contains("<message>Error with &lt;xml&gt; and &amp; characters</message>"));
  }

  @Test
  void testNoSoapFaultInMessage() {
    String normalResponse = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <response>
              <status>success</status>
            </response>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(normalResponse);

    assertFalse(result.hasFaultInfo());
    assertNull(result.faultCode());
    assertNull(result.faultString());
    assertNull(result.detail());
  }

  @Test
  void testNullMessageBody() {
    SoapFaultInfo result = extractSoapFault.extractSoapFault(null);

    assertFalse(result.hasFaultInfo());
    assertNull(result.faultCode());
    assertNull(result.faultString());
    assertNull(result.detail());
  }

  @Test
  void testEmptyMessageBody() {
    SoapFaultInfo result = extractSoapFault.extractSoapFault("");

    // Should return a fault info with error message in faultString
    assertTrue(result.hasFaultInfo());
    assertNull(result.faultCode());
    assertNotNull(result.faultString());
    assertTrue(result.faultString().startsWith("Unknown SOAPFault"));
    assertNull(result.detail());
  }

  @Test
  void testInvalidXmlReturnsErrorFaultInfo() {
    String invalidXml = "<soapenv:Envelope><soapenv:Fault><invalid";

    SoapFaultInfo result = extractSoapFault.extractSoapFault(invalidXml);

    // Should return a fault info with error message in faultString
    assertTrue(result.hasFaultInfo());
    assertNull(result.faultCode());
    assertNotNull(result.faultString());
    assertTrue(result.faultString().startsWith("Unknown SOAPFault"));
    assertNull(result.detail());
  }

  @Test
  void testSoapFaultWithMultipleNamespaces() {
    String soapFault = """
        <soap:Envelope
          xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xmlns:xsd="http://www.w3.org/2001/XMLSchema">
          <soap:Body>
            <soap:Fault>
              <faultcode>soap:Server</faultcode>
              <faultstring>Application error</faultstring>
              <detail>
                <error xmlns="http://example.org" xsi:type="xsd:string">
                  <code>APP_ERROR</code>
                  <description>An application error occurred</description>
                </error>
              </detail>
            </soap:Fault>
          </soap:Body>
        </soap:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soap:Server", result.faultCode());
    assertEquals("Application error", result.faultString());
    assertNotNull(result.detail());
    assertTrue(result.detail().contains("APP_ERROR"));
    assertTrue(result.detail().contains("An application error occurred"));
  }

  @Test
  void testSoapFaultWithNestedElements() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Client</faultcode>
              <faultstring>Validation error</faultstring>
              <detail>
                <ValidationErrors>
                  <Error>
                    <Field>username</Field>
                    <Message>Username is required</Message>
                  </Error>
                  <Error>
                    <Field>password</Field>
                    <Message>Password must be at least 8 characters</Message>
                  </Error>
                </ValidationErrors>
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Client", result.faultCode());
    assertEquals("Validation error", result.faultString());
    assertNotNull(result.detail());
    // Verify nested structure is preserved
    assertTrue(result.detail().contains("ValidationErrors"));
    assertTrue(result.detail().contains("<Field>username</Field>"));
    assertTrue(result.detail().contains("<Field>password</Field>"));
    assertTrue(result.detail().contains("Username is required"));
    assertTrue(result.detail().contains("Password must be at least 8 characters"));
  }

  @Test
  void testSoapFaultCodeOnly() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Server</faultcode>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Server", result.faultCode());
    assertNull(result.faultString());
    assertNull(result.detail());
  }

  @Test
  void testSoapFaultStringOnly() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultstring>Something went wrong</faultstring>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertTrue(result.hasFaultInfo());
    assertNull(result.faultCode());
    assertEquals("Something went wrong", result.faultString());
    assertNull(result.detail());
  }

  @Test
  void testEmptySoapFault() {
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault/>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    SoapFaultInfo result = extractSoapFault.extractSoapFault(soapFault);

    assertFalse(result.hasFaultInfo());
  }

  @Test
  void testSerializeElementExceptionHandling() throws Exception {
    // Test that when getTransformerFactory throws an exception,
    // serializeElement falls back to returning text content
    String soapFault = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <soapenv:Fault>
              <faultcode>soapenv:Server</faultcode>
              <faultstring>Test fault</faultstring>
              <detail>
                <errorInfo>
                  <code>ERR001</code>
                  <message>Error message text</message>
                </errorInfo>
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
        """;

    // Create a spy of the extractor
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());

    // Mock getTransformerFactory to throw an exception
    doThrow(new TransformerConfigurationException("Transformer configuration failed"))
        .when(extractorSpy).getTransformerFactory();

    // Execute extraction
    SoapFaultInfo result = extractorSpy.extractSoapFault(soapFault);

    // Verify that the fault was still extracted despite the exception
    assertTrue(result.hasFaultInfo());
    assertEquals("soapenv:Server", result.faultCode());
    assertEquals("Test fault", result.faultString());
    assertNotNull(result.detail());

    // Verify that the detail contains the text content (fallback behavior)
    // When serialization fails, it should return the text content of the detail element
    assertTrue(result.detail().contains("ERR001"));
    assertTrue(result.detail().contains("Error message text"));

    // Verify that getTransformerFactory was called (attempted serialization)
    verify(extractorSpy, atLeastOnce()).getTransformerFactory();
  }

  @Test
  void testProcessWithHttp500AndValidFault() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    SoapFaultInfo mockFaultInfo = new SoapFaultInfo("soap:Server", "Internal Server Error", "<detail>Error details</detail>");
    doReturn(mockFaultInfo).when(extractorSpy).extractSoapFault(anyString());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy).extractSoapFault("mock body");
    assertEquals("soap:Server", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertEquals("Internal Server Error", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertEquals("<detail>Error details</detail>", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithHttp500AndPartialFaultInfo() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    SoapFaultInfo mockFaultInfo = new SoapFaultInfo(null, "Error occurred", null);
    doReturn(mockFaultInfo).when(extractorSpy).extractSoapFault(anyString());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy).extractSoapFault("mock body");
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertEquals("Error occurred", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithHttp500AndNoFaultInfo() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    SoapFaultInfo mockFaultInfo = new SoapFaultInfo(null, null, null);
    doReturn(mockFaultInfo).when(extractorSpy).extractSoapFault(anyString());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy).extractSoapFault("mock body");
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithNonHttp500Response() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy, never()).extractSoapFault(anyString());
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithHttp404Response() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy, never()).extractSoapFault(anyString());
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithNoHttpResponseCode() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setBody("mock body");

    extractorSpy.process(exchange);

    verify(extractorSpy, never()).extractSoapFault(anyString());
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithHttp500AndNullBody() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    SoapFaultInfo mockFaultInfo = new SoapFaultInfo(null, null, null);
    doReturn(mockFaultInfo).when(extractorSpy).extractSoapFault(null);
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.getIn().setBody(null);

    extractorSpy.process(exchange);

    verify(extractorSpy).extractSoapFault(null);
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertNull(exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }

  @Test
  void testProcessWithHttp500AndAllFaultElements() throws Exception {
    ExtractSoapFaultImpl extractorSpy = spy(new ExtractSoapFaultImpl());
    SoapFaultInfo mockFaultInfo = new SoapFaultInfo(
        "soap:Client",
        "VP004 [NTjP] Det finns inget vägval",
        "<detail><detailString>No receiverId found</detailString></detail>"
    );
    doReturn(mockFaultInfo).when(extractorSpy).extractSoapFault(anyString());
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);
    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.getIn().setBody("<soap:Envelope>...</soap:Envelope>");

    extractorSpy.process(exchange);

    verify(extractorSpy).extractSoapFault("<soap:Envelope>...</soap:Envelope>");
    assertEquals("soap:Client", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE));
    assertEquals("VP004 [NTjP] Det finns inget vägval", exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING));
    assertEquals("<detail><detailString>No receiverId found</detailString></detail>",
                 exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL));
  }
}
