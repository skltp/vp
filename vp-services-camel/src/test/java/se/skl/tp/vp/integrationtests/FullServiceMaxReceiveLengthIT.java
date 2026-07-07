package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP009;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import jakarta.xml.soap.SOAPBody;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

/**
 * Verifies behavior when a producer response exceeds {@code vp.max.receive.length}.
 */
@CamelSpringBootTest
@SpringBootTest
@TestPropertySource(
    locations = {"classpath:application.properties", "classpath:vp-messages.properties"},
    properties = {"vp.max.receive.length=4096"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
@SuppressWarnings("SpringBootApplicationProperties")
public class FullServiceMaxReceiveLengthIT extends LeakDetectionBaseTest {

  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTP_PRODUCER_URL);
    TestLogAppender.clearEvents();
  }

  @Test
  void producerResponseExceedingMaxReceiveLengthShouldGiveSoapFault() {
    // Producer answers with a body far larger than vp.max.receive.length (4096 bytes).
    mockProducer.setResponseBody("<data>" + "A".repeat(100_000) + "</data>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, "dev_env");
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");

    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull(soapBody, "Expected a SOAP Fault response");
    assertStringContains(soapBody.getFault().getFaultString(), "VP009");
    assertStringContains(soapBody.getFault().getFaultString(), "Fel vid kontakt med tjänsteproducenten.");

    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    String errorLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_ERROR, 0);
    assertNotNull(errorLogMsg);
    assertStringContains(errorLogMsg, VP009.getVpDigitErrorCode());

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "VP009");
    assertStringContains(respOutLogMsg, "CamelHttpResponseCode\":\"500");
  }

}
