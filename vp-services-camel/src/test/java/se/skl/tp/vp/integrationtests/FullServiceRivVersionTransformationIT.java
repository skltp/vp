package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_RIV20;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_RIV21;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv20Request;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.xmlunit.matchers.CompareMatcher;
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
public class FullServiceRivVersionTransformationIT extends LeakDetectionBaseTest {
  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;


  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/GetActivitiesResponder";

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @BeforeEach
  public void before() {
    try {
      mockProducer.start(HTTP_PRODUCER_URL);
    } catch (Exception e) {
      e.printStackTrace();
    }
    testLogAppender.clearEvents();
  }

  @Test
  public void riv20To21TransformationTest(){
    mockProducer.setResponseBody("<camel works fine!/>");

    String indata = createGetActivitiesRiv20Request(RECEIVER_RIV21);
    String response = testConsumer.sendHttpsRequestToVP(indata, new HashMap<>());
    assertEquals("<camel works fine!/>", response);

    String inBody = mockProducer.getInBody();
    assertNotEquals("", inBody);
    assertThat(inBody, CompareMatcher.isSimilarTo(createGetActivitiesRiv21Request(RECEIVER_RIV21)));
//    assertStringContains(inBody, String.format("LogicalAddress>%s",RECEIVER_RIV21));
//    assertStringContains(inBody, "=\"http://schemas.xmlsoap.org/soap/envelope/\"");

    assertEquals(0, testLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));

    String reqInLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertStringContains(reqInLogMsg, "-rivversion=rivtabp20");

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "-rivversion=RIVTABP21");

  }

  @Test
  public void riv21To20TransformationTest(){
    mockProducer.setResponseBody("<camel works fine!/>");

    String response = testConsumer.sendHttpsRequestToVP(createGetActivitiesRiv21Request(RECEIVER_RIV20), new HashMap<>());
    assertEquals("<camel works fine!/>", response);

    String inBody = mockProducer.getInBody();
    assertThat(inBody, CompareMatcher.isSimilarTo(createGetActivitiesRiv20Request(RECEIVER_RIV20)));

    assertEquals(0, testLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));

    String reqInLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertStringContains(reqInLogMsg, "-rivversion=rivtabp21");

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "-rivversion=RIVTABP20");

  }

}
