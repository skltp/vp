package se.skl.tp.vp.integrationtests.httpheader;

import static se.skl.tp.vp.constants.HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID;
import static se.skl.tp.vp.constants.HttpHeaders.X_SKLTP_CORRELATION_ID;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CONSUMER;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CORRELATION_ID;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_SENDER;

import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty4.http.NettyHttpOperationFailedException;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.logging.LogExtraInfoBuilder;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.TestSoapRequests;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = TestBeanConfiguration.class)
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@StartTakService
public class HttpRequestHeadersIT extends CamelTestSupport {

  @Value("${" + PropertyConstants.VP_HEADER_USER_AGENT + "}")
  private String vpHeaderUserAgent;

  @Value("${" + PropertyConstants.VP_HEADER_CONTENT_TYPE + "}")
  private String headerContentType;

  @Value("${" + PropertyConstants.VP_INSTANCE_ID + "}")
  private String vpInstanceId;

  @Value("${" + PropertyConstants.VP_HTTP_ROUTE_URL + "}")
  private String httpRoute;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Autowired private CamelContext camelContext;

  private static boolean isContextStarted = false;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @Before
  public void setUp() throws Exception {
    if (!isContextStarted) {
      addConsumerRoute(camelContext);
      camelContext.start();
      isContextStarted = true;
    }
    resultEndpoint.reset();
  }

  @Test
  public void checkSoapActionSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    String s = (String) resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.SOAP_ACTION);
    assertEquals("action", s);
  }

  @Test
  public void checkHeadersSetByConfigTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    assertEquals(vpInstanceId, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_VP_INSTANCE_ID));
    assertEquals(headerContentType, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.HEADER_CONTENT_TYPE));
    assertEquals(vpHeaderUserAgent, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.HEADER_USER_AGENT));
    assertEquals(TEST_SENDER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_VP_SENDER_ID));
  }

  @Test
  public void checkCorrelationIdPropagatedWhenIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithMembers());
    assertEquals(TEST_CORRELATION_ID, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_SKLTP_CORRELATION_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "LogMessage=req-in", "BusinessCorrelationId=" + TEST_CORRELATION_ID);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "LogMessage=resp-out", X_SKLTP_CORRELATION_ID + "=" + TEST_CORRELATION_ID);
  }

  @Test
  public void checkXrivtaOriginalConsumerIdPropagatedWhenIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeadersWithMembers());
    assertEquals(TEST_CONSUMER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "LogMessage=req-in", LogExtraInfoBuilder.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "=" + TEST_CONSUMER);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "LogMessage=resp-out", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "=" + TEST_CONSUMER);
  }

  @Test
  public void checkCorrelationIdPropagatedWithoutIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    String s = (String) resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_SKLTP_CORRELATION_ID);
    assertNotNull(s);
    assertNotEquals(TEST_CORRELATION_ID, s);
    assertTrue(s.length() > 20);
    assertLogExistAndContainsMessages(MessageInfoLogger.REQ_IN, "LogMessage=req-in", "BusinessCorrelationId=" + s);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "LogMessage=resp-out", X_SKLTP_CORRELATION_ID + "=" + s);
  }

  @Test
  public void checkXrivtaOriginalConsumerIdPropagatedWithoutIncomingHeaderSetTest() {
    template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, HeadersUtil.createHttpHeaders());
    assertEquals(TEST_SENDER, resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID));

    String reqInLogMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    assertStringContains(reqInLogMsg, "LogMessage=req-in");
    boolean b = reqInLogMsg.contains(LogExtraInfoBuilder.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID);
    assertFalse(b);
    assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "LogMessage=resp-out", X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + "=" + TEST_SENDER);
  }

  @Test
  public void checkSenderNotAllowedToSetXrivtaOriginalConsumer() {
    Map<String, Object> headers = HeadersUtil.createHttpHeadersWithMembers();
    headers.put(HttpHeaders.X_VP_SENDER_ID, "SENDER3");
    try {
      template.sendBodyAndHeaders(TestSoapRequests.GET_NO_CERT_HTTP_SOAP_REQUEST, headers);
    } catch (CamelExecutionException e) {
      NettyHttpOperationFailedException ne = (NettyHttpOperationFailedException) e.getExchange().getException();
      String err = ne.getContentAsString();
      assert(err.contains("VP013 Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid"));
      assertLogExistAndContainsMessages(MessageInfoLogger.RESP_OUT, "LogMessage=resp-out",
          "VP013 Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid");
    }
  }

  private void assertLogExistAndContainsMessages(String logger, String msg1, String msg2) {
    assertEquals(1, testLogAppender.getNumEvents(logger));
    String logMsg = testLogAppender.getEventMessage(logger, 0);
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
                .to("netty4-http:" + httpRoute);
            // Address below from tak-vagval-test.xml
            from("netty4-http:http://localhost:19000/vardgivare-b/tjanst2")
                .routeDescription("producer")
                .to("mock:result");
          }
        });
  }
}
