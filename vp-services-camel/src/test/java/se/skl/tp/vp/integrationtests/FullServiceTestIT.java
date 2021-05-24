package se.skl.tp.vp.integrationtests;

import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRiv20UTF16Request;

import io.undertow.util.FileUtils;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
public class FullServiceTestIT extends LeakDetectionBaseTest {

  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";
  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @Autowired
  MockProducer mockHttpsProducer;

  @Value("${vp.http.route.url}")
  String vpHttpUrl;

  @Value("${vp.https.route.url}")
  String vpHttpsUrl;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @Value("${http.forwarded.header.xfor}")
  String forwardedHeaderFor;

  @Value("${http.forwarded.header.host}")
  String forwardedHeaderHost;

  @Value("${http.forwarded.header.port}")
  String forwardedHeaderPort;

  @Value("${http.forwarded.header.proto}")
  String forwardedHeaderProto;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @BeforeEach
  public void before() {
    try {
      mockProducer.start(HTTP_PRODUCER_URL);
      mockHttpsProducer.start(HTTPS_PRODUCER_URL + "?sslContextParameters=#outgoingSSLContextParameters&ssl=true");
    } catch (Exception e) {
      e.printStackTrace();
    }
    testLogAppender.clearEvents();
  }

  @Test
  public void callHttpsVPEndpoint2HttpProducerHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    Map<String, Object> producerheaders = mockProducer.getInHeaders();

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpsUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTP, HTTP_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid_in=originalid");
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=originalid");
  }

  @Test
  public void callHttpVPEndpoint2HttpsProducerHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);
    String response2 = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void testVagvalContainingWsdlQuery() {
    mockHttpsProducer.setResponseBody("<mocked https answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest("HttpsProducerWsdl"), headers);
    assertEquals("<mocked https answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void callHttpVPEndpointDeclaredUTF16ButIsUTF8() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void callHttpVPEndpointDeclaredUTF16ButIsUTF8WithContentTypeSet() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-8");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void callHttpVPEndpointUTF16() throws UnsupportedEncodingException {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-16");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes("UTF-16"), headers);
    String response = new String(byteResponse, "UTF-16");

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void callHttpVPEndpointUTF16NoContentTypeSet() throws UnsupportedEncodingException {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes("UTF-16"), headers);
    String response = new String(byteResponse, "UTF-16");

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertTrue(!respOutLogMsg.contains("-originalServiceconsumerHsaid_in"));
  }

  @Test
  public void callWithUTF16ShouldGenerateUTF8CallToProducer() throws UnsupportedEncodingException {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-16");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes("UTF-16"), headers);
    String response = new String(byteResponse, "UTF-16");

    assertEquals("<mocked answer/>", response);

    String inContentType = mockProducer.getInHeader("Content-Type");
    assertStringContains(inContentType, "UTF-8");

    String xmlEncoding = mockProducer.getInBodyXmlReader().getEncoding(); ;
    assertEquals("UTF-8", xmlEncoding);

  }

  @Test
  public void callHttpVPLargePayloadHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String largeRequest = FileUtils
        .readFile(getClass().getClassLoader().getResource("testfiles/ProcessNotificationLargePayload.xml"));
    String response = testConsumer.sendHttpRequestToVP(largeRequest, headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpUrl);
    assertStringContains(respOutLogMsg,
        "-servicecontract_namespace=urn:riv:itintegration:engagementindex:ProcessNotificationResponder:1");
  }

  @Test
  public void callHttpsVPLargePayloadHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    String largeRequest = FileUtils
        .readFile(getClass().getClassLoader().getResource("testfiles/ProcessNotificationLargePayload.xml"));
    String response = testConsumer.sendHttpsRequestToVP(largeRequest, headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpsUrl);
    assertStringContains(respOutLogMsg,
        "-servicecontract_namespace=urn:riv:itintegration:engagementindex:ProcessNotificationResponder:1");
  }

  /**
   * Test for scenario where a reverse-proxy/loadbalancer sits in front of VP and is required to forward original request info to
   * VP for:
   * <ol>
   * <li>X-Forwarded-Proto</li>
   * <li>X-Forwarded-Host</li>
   * <li>X-Forwarded-Port</li>
   * <li>X-Forwarded-For</li>
   * </ol>
   * <p>The information is needed for:
   * <ul>
   * <li>re-writing URL's in WSDL's returned for WSDL lookups using ?wsdl</li>
   * <li>logging/tracing</li>
   * </ul>
   */
  @Test
  public void testLoadBalancerXForwardedInfo() throws Exception {

    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(forwardedHeaderFor, "1.2.3.4");
    headers.put(forwardedHeaderProto, "https");
    headers.put(forwardedHeaderHost, "skltp-lb.example.org");
    headers.put(forwardedHeaderPort, "443");

    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String reqInLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertStringContains(reqInLogMsg, "-senderIpAdress=1.2.3.4");
    assertStringContains(reqInLogMsg, "-httpXForwardedProto=https");
    assertStringContains(reqInLogMsg, "-httpXForwardedHost=skltp-lb.example.org");
    assertStringContains(reqInLogMsg, "-httpXForwardedPort=443");

  }

  @Test
  public void getWsdlByHttpsHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    String response = testConsumer
        .sendHttpsRequestToVP("/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl", null, headers);

    assertStringContains(response,
        "targetNamespace=\"urn:riv:clinicalprocess:healthcond:certificate:GetCertificate:2:rivtabp21\"");
    assertStringContains(response,
        "<xs:import schemaLocation=\"https://localhost:1028/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd\" namespace=\"urn:riv:itintegration:registry:1\"/>");

  }

  @Test
  public void getWsdlByHttpHappyDays() {
    String response = testConsumer
        .sendHttpRequestToVP("/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:clinicalprocess:healthcond:certificate:GetCertificate:2:rivtabp21\"");
    assertStringContains(response,
        "<xs:import schemaLocation=\"http://localhost:12312/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd\" namespace=\"urn:riv:itintegration:registry:1\"/>");

  }

  @Test
  public void getXsdByHttpsHappyDays() {
    String response = testConsumer
        .sendHttpsRequestToVP(
            "clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:itintegration:registry:1\"");
  }

  @Test
  public void getXsdByHttpHappyDays() {
    String response = testConsumer
        .sendHttpRequestToVP(
            "clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:itintegration:registry:1\"");
  }

  private void assertMessageLogsExists() {
    assertEquals(0, testLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_OUT));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.RESP_IN));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
  }

  private void assertExtraInfoLog(String respOutLogMsg, String expectedReceiverId, String expectedProducerUrl) {
    assertStringContains(respOutLogMsg, "-senderIpAdress=");
    assertStringContains(respOutLogMsg,
        "-servicecontract_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");
    assertStringContains(respOutLogMsg, "-senderid=tp");
    assertStringContains(respOutLogMsg, "-receiverid=" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "-endpoint_url=" + expectedProducerUrl);
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "-wsdl_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20");
    assertStringContains(respOutLogMsg, "-rivversion=rivtabp20");
    assertStringContains(respOutLogMsg, "-time.producer=");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=" + expectedReceiverId);
  }

}
