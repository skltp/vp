package se.skl.tp.vp.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.RoutingInfoUtil.createRoutingInfo;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV20;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.httpheader.SenderIpExtractor;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.service.TakCacheService;
import se.skl.tp.vp.util.TestLogAppender;
import se.skltp.takcache.BehorigheterCache;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.VagvalCache;

@CamelSpringBootTest
@SpringBootTest(properties = {
    "timeout.json.file=notExisting.json"
})
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ProducerTimeoutWithoutConfigTest {

  @EndpointInject("mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce("direct:start")
  protected ProducerTemplate template;

  @Autowired SenderIpExtractor senderIpExtractor;

  @Autowired CamelContext camelContext;

  @MockBean TakCache takCache;
  @MockBean
  BehorigheterCache behorigheterCache;
  @MockBean
  VagvalCache vagvalCache;

  @Autowired TimeoutConfiguration timeoutConfiguration;

  @Autowired TakCacheService takCacheService;

  private static boolean isContextStarted = false;

  @BeforeEach
  public void setUp() throws Exception {
    if (!isContextStarted) {
      createRoute(camelContext);
      camelContext.start();
      Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());
      takCacheService.refresh();
    }
    resultEndpoint.reset();
    TestLogAppender.clearEvents();
    isContextStarted = true;
  }

  @Test
  public void noConfigAtAllShouldGetDefaultTimeoutInReqOutTest() throws Exception {
    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo("http://localhost:12123/vp", RIV20));
    Mockito.when(vagvalCache.getRoutingInfo("urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1",
            "UnitTest")).thenReturn(list);
    Mockito.when(behorigheterCache.isAuthorized("UnitTest","urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1",
            "UnitTest")).thenReturn(true);
    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    resultEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);
    resultEndpoint.assertIsSatisfied();
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_OUT));
    String reqOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_OUT, 0);
    assertStringContains(reqOutLogMsg, "CamelNettyRequestTimeout\":\"29000");
  }

  private void createRoute(CamelContext camelContext) throws Exception {
    camelContext.addRoutes( new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start")
                .setHeader(HttpHeaders.X_VP_SENDER_ID, constant("UnitTest"))
                .setHeader(HttpHeaders.X_VP_INSTANCE_ID, constant("dev_env"))
                .setHeader("X-Forwarded-For", constant("1.2.3.4"))
                .to("netty-http:http://localhost:12312/vp?throwExceptionOnFailure=false")
                .to("mock:result");
        ;
        from("netty-http:http://localhost:12123/vp")
                .process(
                        (Exchange exchange) -> {
                          Thread.sleep(1000);
                        });
      }
    });
  }
}
