package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import se.skl.tp.vp.util.JunitUtil;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
@SpringBootTest
@StartTakService
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class FullServiceStandardVagvalIT extends LeakDetectionBaseTest {

  public static final String ANSWER_FROM_DEFAULT_PRODUCER = "<Answer from default producer>";
  public static final String ANSWER_FROM_EXPLICIT_PRODUCER = "<Answer from explicit producer>";
  public static final String ANSWER_FROM_HSATREE_PRODUCER = "<Answer from hsa tree producer>";
  public static final String EVENT_ACTION = "event.action=\"([^\"]+)\"";
  public static final String LABELS_SENDERID = "labels.senderid=\"([^\"]+)\"";
  public static final String LABELS_RECEIVERID = "labels.receiverid=\"([^\"]+)\"";
  public static final String URL_ORIGINAL = "url.original=\"([^\"]+)\"";
  public static final String LABELS_ROUTER_VAGVAL_TRACE = "labels.routerVagvalTrace=\"([^\"]+)\"";
  public static final String LABELS_ROUTER_BEHORIGHET_TRACE = "labels.routerBehorighetTrace=\"([^\"]+)\"";
  public static final String LABELS_ERROR_CODE = "labels.errorCode=\"([^\"]+)\"";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer defaultRoutedProducer;

  @Autowired
  MockProducer explicitlyRoutedProducer;

  @Autowired
  MockProducer hsaTreeRoutedProducer;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @BeforeEach
  void before() throws Exception {
    defaultRoutedProducer.start("http://localhost:1900/default/GetActivitiesResponder");
    explicitlyRoutedProducer.start("http://localhost:1900/explicit/GetActivitiesResponder");
    TestLogAppender.clearEvents();
  }

  @Test
  void testDefaultBehorighetAndDefaultRouting() {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, EVENT_ACTION, "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_SENDERID, "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_RECEIVERID, "AnyReceiver", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, URL_ORIGINAL, "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_VAGVAL_TRACE, "AnyReceiver,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_BEHORIGHET_TRACE, "AnyReceiver,(default),*", 1);
  }

  @Test
  void testDefaultBehorighetAndExplicitRouting() {
    explicitlyRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request(RECEIVER_HTTP), headers);

    assertEquals(ANSWER_FROM_EXPLICIT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, EVENT_ACTION, "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_SENDERID, "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_RECEIVERID, "HttpProducer", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, URL_ORIGINAL, "http://localhost:1900/explicit/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_VAGVAL_TRACE, "HttpProducer", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_BEHORIGHET_TRACE, "HttpProducer,(default),*", 1);
  }

  @Test
  void testDefaultVagvalButNoDefaultBehorighetShouldGiveVP007() {

    explicitlyRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"AnySender");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertStringContains(response, "VP007" );

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, EVENT_ACTION, "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_SENDERID, "AnySender", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_RECEIVERID, "AnyReceiver", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, URL_ORIGINAL, "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_VAGVAL_TRACE, "AnyReceiver,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_BEHORIGHET_TRACE, "AnyReceiver,(default),*,(parent),SE", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ERROR_CODE, "VP007", 1);

  }

  @Test
  void testDefaultVagvalAndDirectBehorighetShouldGiveDefaultRouting() {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"tp");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("GetActivitiesReceiverWithNoExplicitVagval"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, EVENT_ACTION, "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_SENDERID, "tp", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_RECEIVERID, "GetActivitiesReceiverWithNoExplicitVagval", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, URL_ORIGINAL, "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_VAGVAL_TRACE, "GetActivitiesReceiverWithNoExplicitVagval,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_BEHORIGHET_TRACE, "GetActivitiesReceiverWithNoExplicitVagval", 1);
  }

  @Test
  void testHsaRoutingShouldBeDoneBeforeStandardVagval() throws Exception {
    hsaTreeRoutedProducer.start("http://localhost:1900/treeclimbing/GetActivitiesResponder");
    hsaTreeRoutedProducer.setResponseBody(ANSWER_FROM_HSATREE_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("SE0000000001-1234"), headers);

    assertEquals(ANSWER_FROM_HSATREE_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, EVENT_ACTION, "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_SENDERID, "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_RECEIVERID, "SE0000000001-1234", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, URL_ORIGINAL, "http://localhost:1900/treeclimbing/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_VAGVAL_TRACE, "SE0000000001-1234,(parent),SE0000000002-1234,SE0000000003-1234", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, LABELS_ROUTER_BEHORIGHET_TRACE, "SE0000000001-1234,(default),*", 1);
  }

}

