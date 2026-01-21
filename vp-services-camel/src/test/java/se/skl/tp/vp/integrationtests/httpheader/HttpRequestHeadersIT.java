package se.skl.tp.vp.integrationtests.httpheader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.constants.HttpHeaders.*;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CONSUMER;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CORRELATION_ID;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_SENDER;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.TestSoapRequests;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class)
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@StartTakService
class HttpRequestHeadersIT {
  @Value("${" + PropertyConstants.VP_HEADER_USER_AGENT + "}")
  private String vpHeaderUserAgent;

  @Value("${" + PropertyConstants.VP_HEADER_CONTENT_TYPE + "}")
  private String headerContentType;

  @Value("${" + PropertyConstants.VP_INSTANCE_ID + "}")
  private String vpInstanceId;

  @Value("${" + PropertyConstants.VP_HTTP_ROUTE_URL + "}")
  private String httpRoute;

  @EndpointInject("mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce("direct:start")
  protected ProducerTemplate template;

  @Autowired private CamelContext camelContext;

  @Autowired
  ProxyHttpForwardedHeaderProperties proxyHttpForwardedHeaderProperties;

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
    resultEndpoint.reset();
    TestLogAppender.clearEvents();
  }

  @Test
  void checkSoapActionSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    String s = (String) resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.SOAP_ACTION);
    assertEquals("action", s);
  }

  @Test
  void checkHeadersSetByConfigTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    assertEquals(vpInstanceId, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_VP_INSTANCE_ID));
    assertEquals(headerContentType, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.HEADER_CONTENT_TYPE));
    assertEquals(TEST_SENDER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_VP_SENDER_ID));
  }

  @Test
  void checkRIVHeadersPropagatedConfigTest() {
    Map<String, Object> headers = new HashMap<>();
    headers.put("x-rivta-test1", "test1");
    headers.put("x-rivta-TEST2", "TEST2");
    headers.put("x-rivta-123", "123");
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithXRivta(headers));
    for(String key: headers.keySet()){
      assertEquals(headers.get(key), resultEndpoint.getExchanges().get(0).getIn().getHeader(key));
     }
  }

  @Test
  void checkCorrelationIdPropagatedWhenIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithMembers());
    assertEquals(TEST_CORRELATION_ID, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_SKLTP_CORRELATION_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"" + TEST_CORRELATION_ID);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"",  X_SKLTP_CORRELATION_ID + "\":\"" + TEST_CORRELATION_ID);
  }

  @Test
  void checkXrivtaOriginalConsumerIdPropagatedWhenIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithMembers());
    assertEquals(TEST_CONSUMER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "originalServiceconsumerHsaid_in=\"" + TEST_CONSUMER);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "\":\"" + TEST_CONSUMER);
  }

  @Test
  void checkActingOnBehalfOfHeaderPropagatedAndLoggedWhenIncomingHeaderSetTest() {
    Map<String, Object> headers = new HashMap<>();
    headers.put("x-rivta-acting-on-behalf-of-hsaid", "TEST_PRINCIPAL_ID");
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithXRivta(headers));
    assertEquals("TEST_PRINCIPAL_ID", resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_RIVTA_ACTING_ON_BEHALF_OF_HSA_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"",  "actingOnBehalfOfHsaid=\"TEST_PRINCIPAL_ID");
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_RIVTA_ACTING_ON_BEHALF_OF_HSA_ID + "\":\"TEST_PRINCIPAL_ID");
  }

  @Test
  void checkCorrelationIdPropagatedWithoutIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    String s = (String) resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_SKLTP_CORRELATION_ID);
    assertNotNull(s);
    assertNotEquals(TEST_CORRELATION_ID, s);
    assertTrue(s.length() > 20);
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "event.action=\"req-in\"", "trace.id=\"" + s);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_SKLTP_CORRELATION_ID + "\":\"" + s);
  }

  @Test
  void checkXrivtaOriginalConsumerIdPropagatedWithoutIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    assertEquals(TEST_SENDER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));

    String reqInLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertNotNull(reqInLogMsg);
    assertStringContains(reqInLogMsg, "event.action=\"req-in\"");
    boolean b = reqInLogMsg.contains("originalServiceconsumerHsaid_in");
    assertFalse(b);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "\":\"" + TEST_SENDER);
  }

  @Test
  void checkSenderNotAllowedToSetXrivtaOriginalConsumer() {
    String authCertHeadername = proxyHttpForwardedHeaderProperties.getAuth_cert();
    Map<String, Object> headers = HeadersUtil.createHttpProxyHeaders(authCertHeadername);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "SENDER3");
    headers.put(X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, "aTestConsumer");
    try {
      template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, headers);
    } catch (CamelExecutionException e) {
      NettyHttpOperationFailedException ne = (NettyHttpOperationFailedException) e.getExchange().getException();
      String err = ne.getContentAsString();
      assert(err.contains("VP013"));
      assert(err.contains("Enligt tjänsteplattformens konfiguration saknar tjänstekonsumenten rätt att använda headern x-rivta-original-serviceconsumer-hsaid. Kontakta tjänsteplattformsförvaltningen."));
      assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "event.action=\"resp-out\"",
          "Enligt tjänsteplattformens konfiguration saknar tjänstekonsumenten rätt att använda headern x-rivta-original-serviceconsumer-hsaid");
    }
  }

  private void assertLogExistAndContainsMessages(String logger, String msg1, String msg2) {
    assertEquals(1, TestLogAppender.getNumEvents(logger));
    String logMsg = TestLogAppender.getEventMessage(logger, 0);
    assertNotNull(logMsg);
    assertStringContains(logMsg, msg1);
    assertStringContains(logMsg, msg2);
  }

  private void addConsumerRoute(CamelContext camelContext) throws Exception {
    camelContext.addRoutes(
        new RouteBuilder() {
          @Override
          public void configure() {
            from("direct:start")
                .routeId("start")
                .routeDescription("consumer")
                .to("netty-http:" + httpRoute);
            // Address below from tak-vagval-test.xml
            from("netty-http:http://localhost:19000/vardgivare-b/tjanst2")
                .routeDescription("producer")
                .to("mock:result");
          }
        });
  }
}
