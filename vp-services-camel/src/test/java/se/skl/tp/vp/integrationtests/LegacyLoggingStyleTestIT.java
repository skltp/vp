package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTP;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

/**
 * Integration test that verifies the LEGACY logging style configuration (vp.logging.style=LEGACY).
 * This test ensures that when legacy logging is enabled, log messages are formatted in the
 * legacy format with skltp-messages marker and fields like LogMessage, BusinessCorrelationId, etc.
 */
@SuppressWarnings("SpringBootApplicationProperties") // vp.logging.style is not detected by the IDE
@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
@TestPropertySource(properties = {
    "vp.logging.style=LEGACY"
})
public class LegacyLoggingStyleTestIT extends LeakDetectionBaseTest {

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
  void testLegacyLoggingFormatOnHttpsToHttpRequest() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    // Verify LEGACY log format in resp-out message
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertLegacyLogFormat(respOutLogMsg);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
  }

  @Test
  void testLegacyLoggingFormatAllMessageTypes() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    // Verify LEGACY log format in all message types
    String reqInLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_IN, 0);
    assertNotNull(reqInLogMsg);
    assertLegacyLogFormat(reqInLogMsg);
    assertStringContains(reqInLogMsg, "LogMessage=req-in");

    String reqOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_OUT, 0);
    assertNotNull(reqOutLogMsg);
    assertLegacyLogFormat(reqOutLogMsg);
    assertStringContains(reqOutLogMsg, "LogMessage=req-out");

    String respInLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_IN, 0);
    assertNotNull(respInLogMsg);
    assertLegacyLogFormat(respInLogMsg);
    assertStringContains(respInLogMsg, "LogMessage=resp-in");

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertLegacyLogFormat(respOutLogMsg);
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
  }

  static Stream<Arguments> legacyLogFieldParameters() {
    return Stream.of(
        Arguments.of("BusinessCorrelationId=", new String[0]),
        Arguments.of("ExtraInfo=", new String[] {"-senderid=tp", "-receiverid=" + RECEIVER_HTTP}),
        Arguments.of("servicecontract_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", new String[0]),
        Arguments.of("Endpoint=", new String[0]),
        Arguments.of("ComponentId=vp-services-test", new String[0]),
        Arguments.of("MessageId=", new String[0]),
        Arguments.of("Host=", new String[0])
    );
  }

  @ParameterizedTest
  @MethodSource("legacyLogFieldParameters")
  void testLegacyLoggingContainsExpectedFields(String expectedField, String[] additionalExpectedFields) {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, expectedField);

    for (String additionalField : additionalExpectedFields) {
      assertStringContains(respOutLogMsg, additionalField);
    }
  }

  @Test
  void testLegacyLoggingDoesNotUseEcsFormat() {
    mockProducer.setResponseBody("<mocked answer/>");

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "originalid");
    String response = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTP), headers);
    assertEquals("<mocked answer/>", response);

    assertMessageLogsExists();

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    // ECS format would contain event.action, but LEGACY format should NOT
    assertFalse(respOutLogMsg.contains("event.action="), "Legacy format should not contain ECS-style event.action field");
    // ECS format would contain labels.senderid, but LEGACY format uses -senderid
    assertFalse(respOutLogMsg.contains("labels.senderid="), "Legacy format should not contain ECS-style labels.senderid field");
  }

  private void assertMessageLogsExists() {
    assertEquals(0, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_IN));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_OUT));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_IN));
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_OUT));
  }

  private void assertLegacyLogFormat(String logMessage) {
    // Verify the log message contains LEGACY format markers
    assertStringContains(logMessage, "skltp-messages");
    assertStringContains(logMessage, "logEvent-");  // logEvent-info or logEvent-debug
    assertStringContains(logMessage, ".start ***");  // Start marker
    assertStringContains(logMessage, ".end ***");    // End marker
    assertStringContains(logMessage, "LogMessage=");
    assertStringContains(logMessage, "ServiceImpl=");
    assertStringContains(logMessage, "ComponentId=");
    assertStringContains(logMessage, "BusinessCorrelationId=");
    assertStringContains(logMessage, "ExtraInfo=");
  }

}


