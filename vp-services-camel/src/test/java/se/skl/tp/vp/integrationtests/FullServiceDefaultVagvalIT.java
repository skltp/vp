package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.util.TestLogAppender.assertLogMessage;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
@SpringBootTest
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FullServiceDefaultVagvalIT extends LeakDetectionBaseTest {

  public static final String ANSWER_FROM_DEFAULT_PRODUCER = "<Answer from default producer>";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer defaultRoutedProducer;

  @Autowired
  MockProducer explicedRoutedProducer;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @BeforeEach
  void before() throws Exception {
    defaultRoutedProducer.start("http://localhost:1900/default/GetActivitiesResponder");
    explicedRoutedProducer.start("http://localhost:1900/explicit/GetActivitiesResponder");
    TestLogAppender.clearEvents();
  }

  static Stream<Arguments> defaultVagvalWithDelimiterTestData() {
    return Stream.of(
      Arguments.of("FirstReceiverRiv21#SecondReceiverRiv21", "(leaf),SecondReceiverRiv21"),
      Arguments.of("SecondReceiverRiv21#FirstReceiverRiv21", "(leaf),FirstReceiverRiv21"),
      Arguments.of("SecondReceiverRiv21#NotValidReceiver", "(leaf),NotValidReceiver,SecondReceiverRiv21")
    );
  }

  @ParameterizedTest
  @MethodSource("defaultVagvalWithDelimiterTestData")
  void testDefaultVagvalWithDelimiterInReceiver(String receiverId, String expectedLogReceiverIds) {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request(receiverId), headers);
    assertEquals(ANSWER_FROM_DEFAULT_PRODUCER, response);
    assertLogMessage(MessageInfoLogger.RESP_OUT, receiverId, expectedLogReceiverIds, "resp-out", "http://localhost:1900/default/GetActivitiesResponder", "SenderWithDefaultBehorighet");
  }

  @Test
  void testDefaultVagvalWithDelimiterAndTooManyReceivers() {
    defaultRoutedProducer.setResponseBody(ANSWER_FROM_DEFAULT_PRODUCER);
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID,"SenderWithDefaultBehorighet");
    String response = testConsumer.sendHttpRequestToVP(createGetActivitiesRiv21Request("TooManyReceivers#SecondReceiverRiv21#NotValidReceiver"), headers);
    assertTrue(response.contains("VP007"));
    assertTrue(response.contains("Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt. Kontrollera uppgifterna och vid behov, tillse att det beställs konfiguration i aktuell tjänsteplattform."));
  }


}
