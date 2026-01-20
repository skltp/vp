package se.skl.tp.vp.integrationtests.httpheader;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

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
class HttpResponseHeadersIT extends LeakDetectionBaseTest {
  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTPS_PRODUCER_URL);
    TestLogAppender.clearEvents();
  }

  @Test
  void testHeaderSoapActionFilteredInResponse() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader(HttpHeaders.SOAP_ACTION, "mySoapAction");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);
    assertEquals("<mocked answer/>", response);

    assertNull( testConsumer.getReceivedHeader(HttpHeaders.SOAP_ACTION), "SoapAction not expected in response");

    String respInLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_IN, 0);
    assertNotNull(respInLog);
    assertTrue(respInLog.contains("SOAPAction\":\"mySoapAction"));

    String respOutLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLog);
    assertFalse(respOutLog.contains("SOAPAction\":\"mySoapAction"));
  }

  @Test
  void testHeaderMULE_XFilteredInResponse() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader("MULE_CORRELATION_GROUP_SIZE", "-1");
    mockProducer.addResponseHeader("MULE_CORRELATION_ID", "1234");
    mockProducer.addResponseHeader("MULE_CORRELATION_SEQUENCE", "2");
    mockProducer.addResponseHeader("MULE_ENCODING", "erty");


    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS));
    assertEquals("<mocked answer/>", response);
    assertNull( testConsumer.getReceivedHeader("MULE_CORRELATION_GROUP_SIZE"), "MULE_CORRELATION_GROUP_SIZE not expected in response");
    assertNull( testConsumer.getReceivedHeader("MULE_CORRELATION_ID"), "MULE_CORRELATION_ID not expected in response");
    assertNull( testConsumer.getReceivedHeader("MULE_CORRELATION_SEQUENCE"), "MULE_CORRELATION_SEQUENCE not expected in response");
    assertNull( testConsumer.getReceivedHeader("MULE_ENCODING"), "MULE_ENCODING not expected in response");

  }

  @Test
  void testHeaderX_MULE_XFilteredInResponse() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader("X-MULE_CORRELATION_GROUP_SIZE", "-1");
    mockProducer.addResponseHeader("X-MULE_CORRELATION_ID", "1234");
    mockProducer.addResponseHeader("X-MULE_CORRELATION_SEQUENCE", "2");

    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS));
    assertEquals("<mocked answer/>", response);

    assertNull( testConsumer.getReceivedHeader("X-MULE_CORRELATION_GROUP_SIZE"), "X-MULE_CORRELATION_GROUP_SIZE not expected in response");
    assertNull( testConsumer.getReceivedHeader("X-MULE_CORRELATION_ID"), "X-MULE_CORRELATION_ID not expected in response");
    assertNull( testConsumer.getReceivedHeader("X-MULE_CORRELATION_SEQUENCE"), "X-MULE_CORRELATION_SEQUENCE not expected in response");
  }

  @Test
  void testThatRandomHeaderIsNotFilteredInResponse() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader("MyRandomHeader", "myRandomValue");

    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS));
    assertEquals("<mocked answer/>", response);
    assertEquals( "myRandomValue", testConsumer.getReceivedHeader("MyRandomHeader"));

    String respInLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_IN, 0);
    assertNotNull(respInLog);
    assertTrue(respInLog.contains("MyRandomHeader\":\"myRandomValue"));

    String respOutLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLog);
    assertTrue(respOutLog.contains("MyRandomHeader\":\"myRandomValue"));

  }

  @Test
  void testCorrectContentTypeInResponse() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader("Content-Type", "text/plain;charset=UTF-8");

    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS));
    assertEquals("<mocked answer/>", response);
    assertEquals( "text/xml; charset=UTF-8", testConsumer.getReceivedHeader("Content-Type"));

    String respInLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_IN, 0);
    assertNotNull(respInLog);
    assertTrue(respInLog.contains("Content-Type\":\"text/plain;charset=UTF-8"));

    String respOutLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLog);
    assertTrue(respOutLog.contains("Content-Type\":\"text/xml; charset=UTF-8"));

  }

  @Test
  void testCorrectContentTypeWhenErrorInResponse() {
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest("Unknown"));
    assertTrue(response.contains("VP004"));
    assertEquals( "text/xml; charset=UTF-8", testConsumer.getReceivedHeader("Content-Type"));

    String respOutLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLog);
    assertTrue(respOutLog.contains("Content-Type\":\"text/xml; charset=UTF-8"));
  }

  @Test
  void testRoutingHistory() {
    mockProducer.setResponseBody("<mocked answer/>");
    mockProducer.addResponseHeader(HttpHeaders.SOAP_ACTION, "mySoapAction");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    headers.put(HttpHeaders.X_RIVTA_ROUTING_HISTORY, "some-server");
    
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);
    assertEquals("<mocked answer/>", response);

    // Since http, vpInstanceId should not have beeen be added to routing history 
    String routingHistory = testConsumer.getReceivedHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY);
    assertEquals("some-server,mock-producer", routingHistory);

    String respInLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_IN, 0);
    assertNotNull(respInLog);
    assertTrue(respInLog.contains("x-rivta-routing-history\":\"some-server,mock-producer"));

    String respOutLog = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLog);
    assertTrue(respOutLog.contains("x-rivta-routing-history\":\"some-server,mock-producer"));
  }
}
