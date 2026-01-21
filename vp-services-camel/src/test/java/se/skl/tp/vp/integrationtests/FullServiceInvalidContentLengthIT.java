package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FullServiceInvalidContentLengthIT extends LeakDetectionBaseTest {

  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTP_PRODUCER_URL + "?nettyHttpBinding=#producerContentLengthManipulator&disconnect=true");
    TestLogAppender.clearEvents();
  }

  @Test
  void producerAnswerWithTooBigContentLengthShouldBeOk() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "skltp-messages");
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid_in=\"originalid\"");
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"originalid\"");
  }

}
