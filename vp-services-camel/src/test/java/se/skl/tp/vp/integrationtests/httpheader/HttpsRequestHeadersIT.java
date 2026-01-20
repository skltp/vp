package se.skl.tp.vp.integrationtests.httpheader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.constants.HttpHeaders.HEADER_USER_AGENT;
import static se.skl.tp.vp.constants.HttpHeaders.SOAP_ACTION;
import static se.skl.tp.vp.constants.HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID;
import static se.skl.tp.vp.constants.HttpHeaders.X_SKLTP_CORRELATION_ID;
import static se.skl.tp.vp.constants.HttpHeaders.X_VP_INSTANCE_ID;
import static se.skl.tp.vp.constants.HttpHeaders.X_VP_SENDER_ID;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CONSUMER;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CORRELATION_ID;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_SENDER;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.httpheader.OutHeaderProcessorImpl;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.TestSoapRequests;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class)
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HttpsRequestHeadersIT {


  @Value("${" + PropertyConstants.PROPAGATE_CORRELATION_ID_FOR_HTTPS + "}")
  private Boolean propagateCorrIdForHttps;

  @Value("${" + PropertyConstants.VP_HTTPS_ROUTE_URL + "}")
  private String httpsRoute;

  @Value("${" + PropertyConstants.VP_HEADER_USER_AGENT + "}")
  private String vpHeaderUserAgent;

  @EndpointInject("mock:result")
  protected MockEndpoint producerResultEndpoint;

  @Produce("direct:start")
  protected ProducerTemplate template;

  @Autowired private OutHeaderProcessorImpl headerProcessor;

  private boolean oldCorrelation;

  @Autowired private CamelContext camelContext;

  private static boolean isContextStarted = false;

  @BeforeAll
  static void startLeakDetection() {
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterAll
  static void verifyNoLeaks() throws Exception {
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @BeforeEach
  void setUp() throws Exception {
    if (!isContextStarted) {
      addConsumerRoute(camelContext);
      camelContext.start();
      isContextStarted = true;
    }
    producerResultEndpoint.reset();
    oldCorrelation = headerProcessor.getPropagateCorrelationIdForHttps();
    TestLogAppender.clearEvents();
  }

  @AfterEach
  void after() {
    headerProcessor.setPropagateCorrelationIdForHttps(oldCorrelation);
  }

  @Test
  void checkSoapActionSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeaders());
    String soapActionHeader = (String) producerResultEndpoint.getExchanges().get(0).getIn().getHeader(SOAP_ACTION);
    assertEquals("action", soapActionHeader);
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", SOAP_ACTION + "\":\"action");
  }

  // CorrelationId...passCorrelationId set to false.
  @Test // with headers set.
  void checkCorrelationIdPropagatedWithIncomingHeaderSetAndPropagateCorrelationSetFalseTest() {
    headerProcessor.setPropagateCorrelationIdForHttps(false);
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeadersWithCorrId());
    assertNull(producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_SKLTP_CORRELATION_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"");
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertFalse(respOutLogMsg.contains(X_SKLTP_CORRELATION_ID));

  }

  @Test // Without headers
  void checkCorrelationIdPropagatedWithoutIncomingHeaderSetAndPropagateCorrelationSetFalseTest() {
    headerProcessor.setPropagateCorrelationIdForHttps(false);
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeaders());
    assertNull(producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_SKLTP_CORRELATION_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"");

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertFalse(respOutLogMsg.contains(X_SKLTP_CORRELATION_ID));
  }

  // CorrelationId...passCorrelationId set to true.
  @Test // With headers set
  void checkCorrelationIdPropagatedWhenIncomingHeaderSetAndPropagateCorrelationSetTrueTest() {
    headerProcessor.setPropagateCorrelationIdForHttps(true);
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeadersWithCorrId());
    assertEquals(TEST_CORRELATION_ID, producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_SKLTP_CORRELATION_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"" + TEST_CORRELATION_ID);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_SKLTP_CORRELATION_ID + "\":\"" + TEST_CORRELATION_ID);
  }

  @Test // Without headers
  void checkCorrelationIdPropagatedWhenNoIncomingHeaderSetAndPropagateCorrelationSetTrueTest() {
    headerProcessor.setPropagateCorrelationIdForHttps(true);
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeaders());
    String s = (String) producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_SKLTP_CORRELATION_ID);
    assertNotNull(s);
    assertNotEquals(TEST_CORRELATION_ID, s);
    assertTrue(s.length() > 20);
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"" + s);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_SKLTP_CORRELATION_ID + "\":\"" + s);
  }

  // OriginalConsumerId
  @Test // With headers set.
  void checkXrivtaOriginalConsumerIdPropagatedWhenIncomingHeaderSet() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeadersWithOriginalServiceConsumerId());
    assertEquals(TEST_CONSUMER, producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "labels.originalServiceconsumerHsaid_in=\"" + TEST_CONSUMER);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "\":\"" + TEST_CONSUMER);
  }

  @Test // Without headers.
  void checkXrivtaConsumerIdPropagatedWhenNoIncomingHeaderTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeaders());
    assertEquals(TEST_SENDER, producerResultEndpoint.getExchanges().get(0).getIn().getHeader(X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));

    String reqInLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertNotNull(reqInLogMsg);
    assertStringContains(reqInLogMsg, "event.action=\"req-in\"");
    boolean b = reqInLogMsg.contains("labels.originalServiceconsumerHsaid_in=\"" + TEST_SENDER);
    assertFalse(b);

    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "\":\"" +TEST_SENDER);
  }

  @Test
  void checkUserAgentGetPropagated() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpsHeaders());
    String s = (String) producerResultEndpoint.getExchanges().get(0).getIn().getHeader(HEADER_USER_AGENT);
    assertEquals(vpHeaderUserAgent, s);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", HEADER_USER_AGENT + "\":\"" + vpHeaderUserAgent);
  }

  private void assertLogExistAndContainsMessages(String logger, String msg1, String msg2) {
    assertEquals(1, TestLogAppender.getNumEvents(logger));
    String logMessage = TestLogAppender.getEventMessage(logger, 0);
    assertNotNull(logMessage);
    assertStringContains(logMessage, msg1);
    assertStringContains(logMessage, msg2);
  }

  @Test
  void testInstanceIdDontGetPropagatedForHttps () {
    //Using http-headers for this one, where X_VP_INSTANCE_ID is set.
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpHeaders());
    negativeHeaderAndLogTest(X_VP_INSTANCE_ID);
  }

  @Test
  void testXvPSenderIdDontGetPropagatedForHttps () {
    //Using http-headers for this one, where X_VP_SENDER_ID is set.
    template.sendBodyAndHeaders(TestSoapRequests.GET_CERT_HTTPS_REQUEST, HeadersUtil.createHttpHeaders());
    negativeHeaderAndLogTest(X_VP_SENDER_ID);
  }

  private void negativeHeaderAndLogTest(String header) {
    assertNull(producerResultEndpoint.getExchanges().get(0).getIn().getHeader(header));
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertFalse(respOutLogMsg.contains(header));
  }

  private void addConsumerRoute(CamelContext camelContext) throws Exception {
    camelContext.addRoutes(
        new RouteBuilder() {
          @Override
          public void configure() {
            from("direct:start")
                .routeId("start")
                .to(
                    "netty-http:"
                        + httpsRoute
                        + "?sslContextParameters=#incomingSSLContextParameters&ssl=true&"
                        + "sslClientCertHeaders=true&needClientAuth=true&matchOnUriPrefix=true");
            // Address below from tak-vagval-test.xml
            from("netty-http:https://localhost:19001/vardgivare-b/tjanst2?sslContextParameters=#SSLContext-default&ssl=true")
                .to("mock:result");
          }
        });
  }
}
