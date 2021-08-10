package se.skl.tp.vp.integrationtests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.requestreader.RequestReaderProcessorXMLEventReaderTest.MTOM_TEXT_1;
import static se.skl.tp.vp.requestreader.RequestReaderProcessorXMLEventReaderTest.MTOM_TEXT_2;
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
public class FullServiceMtomIT extends LeakDetectionBaseTest {

  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

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
    } catch (Exception e) {
      e.printStackTrace();
    }
    testLogAppender.clearEvents();
  }

  @Test
  public void callMtomHappyDays() {
    mockProducer.setResponseBody("<This worked!/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "multipart/related; type=\"application/xop+xml\";start=\"<http://tempuri.org/0>\";boundary=\"uuid:1b56605a-1344-46cd-98b9-4687e64cfe7a+id=393\";start-info=\"text/xml\"\\r\\n");
    String response = testConsumer.sendHttpsRequestToVP(MTOM_TEXT_1, headers);
    assertEquals("<This worked!/>", response);

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpsUrl);

  }

  @Test
  public void callMtomHappyDays2() {
    mockProducer.setResponseBody("<This also worked!/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "multipart/related; type=\"application/xop+xml\";start=\"<http://tempuri.org/0>\";boundary=\"uuid:1b56605a-1344-46cd-98b9-4687e64cfe7a+id=393\";start-info=\"text/xml\"\\r\\n");
    String response = testConsumer.sendHttpsRequestToVP(MTOM_TEXT_2, headers);
    assertEquals("<This also worked!/>", response);

    String respOutLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "Endpoint=" + vpHttpsUrl);

  }

}
