package se.skl.tp.vp.integrationtests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.skl.tp.vp.requestreader.RequestReaderProcessorXMLEventReaderTest.MTOM_TEXT_1;
import static se.skl.tp.vp.requestreader.RequestReaderProcessorXMLEventReaderTest.MTOM_TEXT_2;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

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
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

@SuppressWarnings("HttpUrlsUsage") // Localhost url for testing
@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
public class FullServiceMtomIT extends LeakDetectionBaseTest {

  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";
  public static final String CONTENT_TYPE_HEADER = "multipart/related; type=\"application/xop+xml\";start=\"<http://tempuri.org/0>\";boundary=\"uuid:1b56605a-1344-46cd-98b9-4687e64cfe7a+id=393\";start-info=\"text/xml\"\\r\\n";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @Value("${vp.https.route.url}")
  String vpHttpsUrl;

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTP_PRODUCER_URL);
    TestLogAppender.clearEvents();
  }

  @Test
  void callMtomHappyDays() {
    mockProducer.setResponseBody("<This worked!/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", CONTENT_TYPE_HEADER);
    String response = testConsumer.sendHttpsRequestToVP(MTOM_TEXT_1, headers);
    assertEquals("<This worked!/>", response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpsUrl);

  }

  @Test
  void callMtomHappyDays2() {
    mockProducer.setResponseBody("<This also worked!/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", CONTENT_TYPE_HEADER);
    String response = testConsumer.sendHttpsRequestToVP(MTOM_TEXT_2, headers);
    assertEquals("<This also worked!/>", response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "url.full=\"" + vpHttpsUrl);

  }

}
