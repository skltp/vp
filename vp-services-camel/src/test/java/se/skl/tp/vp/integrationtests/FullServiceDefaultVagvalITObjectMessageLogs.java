package se.skl.tp.vp.integrationtests;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TakMockWebService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.LogExtraInfoBuilder;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.logging.logentry.LogMessageType;
import se.skl.tp.vp.logging.logentry.LogMetadataInfoType;
import se.skl.tp.vp.logging.logentry.LogRuntimeInfoType;
import se.skl.tp.vp.util.JunitUtil;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;

@CamelSpringBootTest
@SpringBootTest(properties = { "message.logger.method=object" })
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FullServiceDefaultVagvalITObjectMessageLogs extends LeakDetectionBaseTest {

  public static final String ANSWER_FROM_DEFAULT_PRODUCER = "<Answer from default producer>";

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
  public void testDefaultVagvalWithDelimiterInReceiver() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("FirstReceiverRiv21#SecondReceiverRiv21"), headers);
    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);
    assertLogMessage("FirstReceiverRiv21#SecondReceiverRiv21", "SecondReceiverRiv21");
  }

  @Test
  public void testDefaultVagvalWithDelimiterInReceiverReversed() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("SecondReceiverRiv21#FirstReceiverRiv21"), headers);
    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);
    assertLogMessage("SecondReceiverRiv21#FirstReceiverRiv21", "FirstReceiverRiv21");
  }

  @Test
  public void testDefaultVagvalWithDelimiterInReceiverJustOneValid() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("SecondReceiverRiv21#NotValidReceiver"), headers);
    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);
    assertLogMessage("SecondReceiverRiv21#NotValidReceiver", "NotValidReceiver,SecondReceiverRiv21");
  }

  @Test
  public void testDefaultVagvalWithDelimiterAndTooManyReceivers() throws Exception {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("TooManyReceivers#SecondReceiverRiv21#NotValidReceiver"), headers);
    assertTrue(response.contains("VP007"));
    assertTrue(response.contains("Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt. Kontrollera uppgifterna och vid behov, tillse att det beställs konfiguration i aktuell tjänsteplattform."));
  }

  private void assertLogMessage(String receiver, String trace) {
    Message respOutLogMsg = TestLogAppender.getEventMessageObject(MessageInfoLogger.RESP_OUT, 0);
    assert respOutLogMsg != null;
    HashMap<Object, Object> messageMap = (HashMap<Object, Object>) ((Object[])respOutLogMsg.getParameters())[0];

    LogMessageType messageInfo = (LogMessageType) messageMap.get("messageInfo");
    assertEquals("resp-out", messageInfo.getMessage());

    HashMap<String, Object> extraInfo = (HashMap<String, Object>) messageMap.get("extraInfo");
    assertEquals("SenderWithDefaultBehorighet", extraInfo.get("senderid"));
    assertEquals("http://localhost:1900/default/GetActivitiesResponder", extraInfo.get("endpoint_url"));
    assertEquals(receiver, extraInfo.get("receiverid"));
    assertEquals("(leaf)", ((String[])extraInfo.get("routerVagvalTrace"))[0]);
    assertEquals("(leaf)", ((String[])extraInfo.get("routerBehorighetTrace"))[0]);
  }

}
