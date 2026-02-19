package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRiv20UTF16Request;

import io.undertow.util.FileUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
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
import se.skl.tp.vp.logging.MessageLogger;
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

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTP_PRODUCER_URL);
    mockHttpsProducer.start(HTTPS_PRODUCER_URL);
    TestLogAppender.clearEvents();
  }

  @Test
  void callHttpsVPEndpoint2HttpProducerHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpsUrl);
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTP, HTTP_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid_in=\"originalid\"");
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"originalid\"");
  }

  @Test
  void callHttpVPEndpoint2HttpsProducerHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);
    String response2 = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);
    assertEquals("<mocked answer/>", response2);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
      assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void testVagvalContainingWsdlQuery() {
    mockHttpsProducer.setResponseBody("<mocked https answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest("HttpsProducerWsdl"), headers);
    assertEquals("<mocked https answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void callHttpVPEndpointDeclaredUTF16ButIsUTF8() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void callHttpVPEndpointDeclaredUTF16ButIsUTF8WithContentTypeSet() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-8");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS), headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void callHttpVPEndpointUTF16() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-16");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes(StandardCharsets.UTF_16), headers);
    String response = new String(byteResponse, StandardCharsets.UTF_16);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void callHttpVPEndpointUTF16NoContentTypeSet() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes(StandardCharsets.UTF_16), headers);
    String response = new String(byteResponse, StandardCharsets.UTF_16);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg, "source.ip=");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_HTTPS, HTTPS_PRODUCER_URL);
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertFalse(respOutLogMsg.contains("labels.originalServiceconsumerHsaid_in"));
  }

  @Test
  void callWithUTF16ShouldGenerateUTF8CallToProducer() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.HEADER_CONTENT_TYPE, "text/xml;charset=UTF-16");
    String payload = createGetCertificateRiv20UTF16Request(RECEIVER_HTTPS);
    byte[] byteResponse = testConsumer.sendHttpRequestToVP(payload.getBytes(StandardCharsets.UTF_16), headers);
    String response = new String(byteResponse, StandardCharsets.UTF_16);

    assertEquals("<mocked answer/>", response);

    String inContentType = mockProducer.getInHeader("Content-Type");
    assertStringContains(inContentType, "UTF-8");

    String xmlEncoding = mockProducer.getInBodyXmlReader().getEncoding();
    assertEquals("UTF-8", xmlEncoding);

  }

  @Test
  void callHttpVPLargePayloadHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    URL resource = getClass().getClassLoader().getResource("testfiles/ProcessNotificationLargePayload.xml");
    assertNotNull(resource);
    String largeRequest = FileUtils.readFile(resource);
    String response = testConsumer.sendHttpRequestToVP(largeRequest, headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpUrl);
    assertStringContains(respOutLogMsg,
        "labels.servicecontract_namespace=\"urn:riv:itintegration:engagementindex:ProcessNotificationResponder:1\"");
  }

  @Test
  void callHttpsVPLargePayloadHappyDays() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    URL resource = getClass().getClassLoader().getResource("testfiles/ProcessNotificationLargePayload.xml");
    assertNotNull(resource);
    String largeRequest = FileUtils.readFile(resource);
    String response = testConsumer.sendHttpsRequestToVP(largeRequest, headers);

    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpsUrl);
    assertStringContains(respOutLogMsg,
        "labels.servicecontract_namespace=\"urn:riv:itintegration:engagementindex:ProcessNotificationResponder:1\"");
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
  void testLoadBalancerXForwardedInfo() {

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

    String reqInLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_IN, 0);
    assertNotNull(reqInLogMsg);
    assertStringContains(reqInLogMsg, "source.ip=\"1.2.3.4\"");
    assertStringContains(reqInLogMsg, "labels.httpXForwardedProto=\"https\"");
    assertStringContains(reqInLogMsg, "labels.httpXForwardedHost=\"skltp-lb.example.org\"");
    assertStringContains(reqInLogMsg, "labels.httpXForwardedPort=\"443\"");

  }

  @Test
  void getWsdlByHttpsHappyDays() {
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
  void getWsdlByHttpHappyDays() {
    String response = testConsumer
        .sendHttpRequestToVP("/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:clinicalprocess:healthcond:certificate:GetCertificate:2:rivtabp21\"");
    assertStringContains(response,
        "<xs:import schemaLocation=\"http://localhost:12312/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd\" namespace=\"urn:riv:itintegration:registry:1\"/>");

  }

  @Test
  void getXsdByHttpsHappyDays() {
    String response = testConsumer
        .sendHttpsRequestToVP(
            "clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:itintegration:registry:1\"");
  }

  @Test
  void getXsdByHttpHappyDays() {
    String response = testConsumer
        .sendHttpRequestToVP(
            "clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd",
            null, new HashMap<>());

    assertStringContains(response,
        "targetNamespace=\"urn:riv:itintegration:registry:1\"");
  }

  private void assertMessageLogsExists() {
    assertEquals(0, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_IN));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_OUT));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_IN));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_OUT));
  }

  private void assertExtraInfoLog(String respOutLogMsg, String expectedReceiverId, String expectedProducerUrl) {
    assertStringContains(respOutLogMsg,
        "labels.servicecontract_namespace=\"urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1\"");
    assertStringContains(respOutLogMsg, "labels.senderid=\"tp\"");
    assertStringContains(respOutLogMsg, "labels.receiverid=\"" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "url.original=\"" + expectedProducerUrl);
    assertStringContains(respOutLogMsg, "labels.routerVagvalTrace=\"" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "labels.wsdl_namespace=\"urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20\"");
    assertStringContains(respOutLogMsg, "labels.rivversion=\"rivtabp20\"");
    assertStringContains(respOutLogMsg, "event.duration=");
    assertStringContains(respOutLogMsg, "labels.routerBehorighetTrace=\"" + expectedReceiverId);
  }

}
