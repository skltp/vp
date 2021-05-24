package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @BeforeEach
  public void before() throws Exception {
    defaultRoutedProducer.start("http://localhost:1900/default/GetActivitiesResponder");
    explicedRoutedProducer.start("http://localhost:1900/explicit/GetActivitiesResponder");
    testLogAppender.clearEvents();
  }

  @Test
  public void testDefaultBehorighetAndDefaultRouting() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "-senderid=SenderWithDefaultBehorighet");
    assertStringContains(respOutLogMsg, "-receiverid=AnyReceiver");
    assertStringContains(respOutLogMsg, "-endpoint_url=http://localhost:1900/default/GetActivitiesResponder");
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=AnyReceiver,(parent)SE,(default)*");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=AnyReceiver,(parent)SE,(default)*");
  }

  @Test
  public void testDefaultBehorighetAndExplicitRouting() throws Exception {
    explicedRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request(RECEIVER_HTTP), headers);

    assertEquals(ANSWER_FROM_EXPLICIT_PRODUCER, response);

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "-senderid=SenderWithDefaultBehorighet");
    assertStringContains(respOutLogMsg, "-receiverid=HttpProducer");
    assertStringContains(respOutLogMsg, "-endpoint_url=http://localhost:1900/explicit/GetActivitiesResponder");
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=HttpProducer\n");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=HttpProducer,(parent)SE,(default)*");
  }

  @Test
  public void testDefaultVagvalButNoDefaultBehorighetShouldGiveVP007() throws Exception {

    explicedRoutedProducer.setResponseBody(ANSWER_FROM_EXPLICIT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"AnySender");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("AnyReceiver"), headers);

    assertStringContains(response, "VP007" );

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "-senderid=AnySender");
    assertStringContains(respOutLogMsg, "-receiverid=AnyReceiver");
    assertStringContains(respOutLogMsg, "-endpoint_url=http://localhost:1900/default/GetActivitiesResponder");
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=AnyReceiver,(parent)SE,(default)*");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=AnyReceiver,(parent)SE,(default)*");
    assertStringContains(respOutLogMsg, "-errorCode=VP007");

  }

  @Test
  public void testDefaultVagvalAndDirectBehorighetShouldGiveDefaultRouting() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"tp");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("GetActivitiesReceiverWithNoExplicitVagval"), headers);

    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "-senderid=tp");
    assertStringContains(respOutLogMsg, "-receiverid=GetActivitiesReceiverWithNoExplicitVagval");
    assertStringContains(respOutLogMsg, "-endpoint_url=http://localhost:1900/default/GetActivitiesResponder");
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=GetActivitiesReceiverWithNoExplicitVagval,(parent)SE,(default)*");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=GetActivitiesReceiverWithNoExplicitVagval");
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

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "-senderid=SenderWithDefaultBehorighet");
    assertStringContains(respOutLogMsg, "-receiverid=SE0000000001-1234");
    assertStringContains(respOutLogMsg, "-endpoint_url=http://localhost:1900/treeclimbing/GetActivitiesResponder");
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=SE0000000001-1234,(parent)SE0000000002-1234,SE0000000003-1234\n");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=SE0000000001-1234,(parent)SE0000000002-1234,SE0000000003-1234\n");
  }

}

