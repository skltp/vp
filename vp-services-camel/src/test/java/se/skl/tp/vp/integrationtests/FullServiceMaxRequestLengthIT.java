package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

/**
 * Verifies behavior when an incoming consumer request exceeds {@code vp.max.receive.length}.
 *
 * <p>Such a request is rejected inside Netty's {@link io.netty.handler.codec.http.HttpObjectAggregator}
 * before any Camel exchange is created. VP shadows that aggregator so that, instead of a bare
 * {@code 413 Request Entity Too Large}, a standardized VP015 SOAP Fault with HTTP status 500 is
 * returned to the consumer.
 */
@CamelSpringBootTest
@SpringBootTest
@TestPropertySource(
    locations = {"classpath:application.properties", "classpath:vp-messages.properties"},
    properties = {"vp.max.receive.length=4096"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
@SuppressWarnings("SpringBootApplicationProperties")
class FullServiceMaxRequestLengthIT extends LeakDetectionBaseTest {

  @Autowired
  TestConsumer testConsumer;

  @BeforeEach
  void before() {
    TestLogAppender.clearEvents();
  }

  @Test
  void consumerRequestExceedingMaxReceiveLengthShouldGiveVp015SoapFault() {
    // Body far larger than vp.max.receive.length (4096 bytes). Content is irrelevant -
    // VP rejects on size before the request is parsed or routed.
    String oversizedRequest = "<data>" + "A".repeat(100_000) + "</data>";

    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, "dev_env");
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");

    String result = testConsumer.sendHttpRequestToVP(oversizedRequest, headers);

    assertNotNull(result, "Expected a SOAP Fault response body");
    assertStringContains(result, "faultstring");
    assertStringContains(result, "VP015");
    assertStringContains(result, "Client");

    String responseCode = testConsumer.getReceivedHeader(Exchange.HTTP_RESPONSE_CODE);
    assertEquals("500", responseCode);
  }

}
