package se.skl.tp.vp.integrationtests.errorhandling;
import static se.skl.tp.vp.integrationtests.errorhandling.AddTemporarySocketProblem.BODY_ON_SECOND_INVOKATION;
import static se.skl.tp.vp.util.soaprequests.RoutingInfoUtil.createRoutingInfo;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV20;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.service.TakCacheService;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = TestBeanConfiguration.class)
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReSendIT extends LeakDetectionBaseTest {

  private static final String MOCK_PRODUCER_ADDRESS = "http://localhost:12126/vp";
  private static final String VP_ADDRESS = "http://localhost:12312/vp";
  private static final String URL_MOCK_ENDPOINT= "mock:result";
  private static boolean isContextStarted = false;


  @MockBean TakCache takCache;

  @Autowired TakCacheService takCacheService;

  @Autowired private CamelContext camelContext;

  @EndpointInject(uri = URL_MOCK_ENDPOINT)
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Before
  public void init() throws Exception {
    if (!isContextStarted) {
      routeFromDirectStartToVp(camelContext);
      //AddTemporarySocketProblem.toProducerOnProducerRoute(camelContext,URL_MOCK_ENDPOINT);
      camelContext.start();
      isContextStarted = true;
    }

    resultEndpoint.reset();
    Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());
    takCacheService.refresh();
  }


  @Test
  public void worksForSingleException() throws InterruptedException {
    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(MOCK_PRODUCER_ADDRESS, RIV20));
    setTakCacheMockResult(list);

    resultEndpoint.setExpectedCount(1);
    resultEndpoint.expectedBodiesReceived(BODY_ON_SECOND_INVOKATION);

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    resultEndpoint.assertIsSatisfied();
  }

  private void routeFromDirectStartToVp(CamelContext camelContext) throws Exception {

    camelContext.addRoutes(
        new RouteBuilder() {
          @Override
          public void configure() {
            from("direct:start")
                .routeDescription("Direct start to vp")
                .id("ConsumerVp")
                .setHeader(HttpHeaders.X_VP_SENDER_ID, constant("UnitTest"))
                .setHeader(HttpHeaders.X_VP_INSTANCE_ID, constant("dev_env"))
                .setHeader("X-Forwarded-For", constant("1.2.3.4"))
                .to("netty-http:" + VP_ADDRESS);
          }
        });
  }


  private void setTakCacheMockResult(List<RoutingInfo> list) {
    Mockito.when(
            takCache.getRoutingInfo(
                "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", "UnitTest"))
        .thenReturn(list);
    Mockito.when(
            takCache.isAuthorized(
                "UnitTest",
                "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1",
                "UnitTest"))
        .thenReturn(true);
  }
}
