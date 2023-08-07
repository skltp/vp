package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.skl.tp.vp.util.JunitUtil.assertMatchRegexGroup;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

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
import se.skl.tp.vp.integrationtests.utils.TakMockWebService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.JunitUtil;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
@SpringBootTest
@StartTakService
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class FullServiceStandardVagvalIT extends LeakDetectionBaseTest {

  public static final String ANSWER_FROM_DEFAULT_PRODUCER = "<Answer from default producer>";
  public static final String ANSWER_FROM_EXPLICIT_PRODUCER = "<Answer from explicit producer>";
  public static final String ANSWER_FROM_HSATREE_PRODUCER = "<Answer from hsa tree producer>";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer defaultRoutedProducer;

  @Autowired
  MockProducer explicedRoutedProducer;

  @Autowired
  MockProducer hsaTreeRoutedProducer;

  @Autowired
  TakMockWebService takMockWebService;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @BeforeEach
  public void before() throws Exception {
    defaultRoutedProducer.start("http://localhost:1900/default/GetActivitiesResponder");
    explicedRoutedProducer.start("http://localhost:1900/explicit/GetActivitiesResponder");
    TestLogAppender.clearEvents();
  }

  @Test
  public void testDefaultBehorighetAndDefaultRouting() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "LogMessage=(.*)", "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-senderid=(.*)", "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-receiverid=(.*)", "AnyReceiver", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-endpoint_url=(.*)", "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerVagvalTrace=(.*)", "AnyReceiver,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerBehorighetTrace=(.*)", "AnyReceiver,(default),*", 1);
  }

  @Test
  public void testDefaultBehorighetAndExplicitRouting() throws Exception {
    explicedRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request(RECEIVER_HTTP), headers);

    assertEquals(ANSWER_FROM_EXPLICIT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "LogMessage=(.*)", "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-senderid=(.*)", "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-receiverid=(.*)", "HttpProducer", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-endpoint_url=(.*)", "http://localhost:1900/explicit/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerVagvalTrace=(.*)", "HttpProducer", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerBehorighetTrace=(.*)", "HttpProducer,(default),*", 1);
  }

  @Test
  public void testDefaultVagvalButNoDefaultBehorighetShouldGiveVP007() throws Exception {

    explicedRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"AnySender");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertStringContains(response, "VP007" );

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "LogMessage=(.*)", "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-senderid=(.*)", "AnySender", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-receiverid=(.*)", "AnyReceiver", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-endpoint_url=(.*)", "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerVagvalTrace=(.*)", "AnyReceiver,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerBehorighetTrace=(.*)", "AnyReceiver,(default),*,(parent),SE", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-errorCode=(.*)", "VP007", 1);

  }

  @Test
  public void testDefaultVagvalAndDirectBehorighetShouldGiveDefaultRouting() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"tp");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("GetActivitiesReceiverWithNoExplicitVagval"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "LogMessage=(.*)", "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-senderid=(.*)", "tp", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-receiverid=(.*)", "GetActivitiesReceiverWithNoExplicitVagval", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-endpoint_url=(.*)", "http://localhost:1900/default/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerVagvalTrace=(.*)", "GetActivitiesReceiverWithNoExplicitVagval,(parent),SE,(default),*", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerBehorighetTrace=(.*)", "GetActivitiesReceiverWithNoExplicitVagval", 1);
  }

  @Test
  public void testHsaRoutingShouldBeDoneBeforeStandardVagval() throws Exception {
    hsaTreeRoutedProducer.start("http://localhost:1900/treeclimbing/GetActivitiesResponder");
    hsaTreeRoutedProducer.setResponseBody(ANSWER_FROM_HSATREE_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("SE0000000001-1234"), headers);

    assertEquals(ANSWER_FROM_HSATREE_PRODUCER, response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "LogMessage=(.*)", "resp-out", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-senderid=(.*)", "SenderWithDefaultBehorighet", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-receiverid=(.*)", "SE0000000001-1234", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-endpoint_url=(.*)", "http://localhost:1900/treeclimbing/GetActivitiesResponder", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerVagvalTrace=(.*)", "SE0000000001-1234,(parent),SE0000000002-1234,SE0000000003-1234", 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "-routerBehorighetTrace=(.*)", "SE0000000001-1234,(default),*", 1);
  }

}

