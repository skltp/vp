package se.skl.tp.vp.errorhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.util.soaprequests.RoutingInfoUtil.createRoutingInfo;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV20;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.exceptions.VPFaultCodeEnum;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.service.TakCacheService;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ErrorInResponseTest extends LeakDetectionBaseTest {

  public static final String REMOTE_EXCEPTION_MESSAGE = "Fel fel fel";
  public static final String REMOTE_SOAP_FAULT =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
          + "  <soapenv:Header/>  <soapenv:Body>    "
          +     "<soapenv:Fault>"
          + "      <faultcode>soapenv:Client</faultcode>"
          + "      <faultstring>VP011 [NTjP Remote] Anrop har gjorts utanför TLS vilket ej är tillåtet. Tjänstekonsumenten ska alltid använda TLS för säker kommunikation.</faultstring>"
          + "      <details> Caller was not on the white list of accepted IP-addresses. IP-address: 84.17.194.105. HTTP header that caused checking: x-vp-sender-id (se.skl.tp.vp.exceptions.VpSemanticException). Message payload is of type: ReversibleXMLStreamReader</details>\n"
          + "    </soapenv:Fault>  </soapenv:Body></soapenv:Envelope>";
  public static final String VP_ADDRESS = "http://localhost:12312/vp";
  public static final String NO_EXISTING_PRODUCER = "http://localhost:12100/vp";
  public static final String MOCK_PRODUCER_ADDRESS = "http://localhost:12126/vp";

  @Autowired
  private CamelContext camelContext;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @MockBean
  TakCache takCache;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @Autowired
  TakCacheService takCacheService;

  private static MockProducer mockProducer;

  private static boolean isContextStarted = false;

  @BeforeEach
  public void setUp() throws Exception {
    if(!isContextStarted){
      mockProducer = new MockProducer(camelContext, MOCK_PRODUCER_ADDRESS);
      addConsumerRoute(camelContext);
      camelContext.start();
      isContextStarted=true;
    }
    resultEndpoint.reset();
    Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());
    takCacheService.refresh();

    mockProducer.setResponseHttpStatus(200);
  }

  @Test //Test för när ett SOAP-fault kommer från Producenten
  public void errorInResponseTest() throws Exception {
    mockProducer.setResponseHttpStatus(200);
    mockProducer.setResponseBody(SoapFaultHelper.generateSoap11FaultWithCause(REMOTE_EXCEPTION_MESSAGE,
        VPFaultCodeEnum.Client));

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(MOCK_PRODUCER_ADDRESS, RIV20));
    setTakCacheMockResult(list);

    resultEndpoint.expectedBodiesReceived(SoapFaultHelper.generateSoap11FaultWithCause(REMOTE_EXCEPTION_MESSAGE, VPFaultCodeEnum.Client));

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    resultEndpoint.assertIsSatisfied();
  }

  @Test //Test för när en Producent inte går att nå
  public void noProducerOnURLResponseTest() throws Exception {
    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(NO_EXISTING_PRODUCER, RIV20));
    setTakCacheMockResult(list);

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
    assertTrue(resultBody.contains("VP009"));
    assertTrue(resultBody.contains("Fel vid kontakt med tjänsteproducenten."));
    assertTrue(resultBody.contains("Error connecting to service producer at address " + NO_EXISTING_PRODUCER));
    resultEndpoint.assertIsSatisfied();
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_ERROR,0);
    assertTrue(respOutLogMsg.contains("CamelHttpResponseCode=500"));
    assertTrue(respOutLogMsg.contains("-sessionErrorTechnicalDescription=java.net.ConnectException: Cannot connect to localhost:12100"));
  }

  @Test //Test för när en Producent svarar med ett tomt svar
  public void emptyResponseTest() throws Exception {
    mockProducer.setResponseBody("");

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(MOCK_PRODUCER_ADDRESS, RIV20));
    setTakCacheMockResult(list);

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
    assertTrue(resultBody.contains("VP009"));
    assertTrue(resultBody.contains("Fel vid kontakt med tjänsteproducenten."));
    assertTrue(resultBody.contains("Error connecting to service producer at address " + MOCK_PRODUCER_ADDRESS));
    resultEndpoint.assertIsSatisfied();
  }

  @Test //Test för när en Producent svarar med annat än SOAP tex ett exception, kontrolleras inte av VP
  public void nonSOAPResponseTest() throws Exception {
    mockProducer.setResponseHttpStatus(200);
    mockProducer.setResponseBody(createExceptionMessage());

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(MOCK_PRODUCER_ADDRESS, RIV20));
    setTakCacheMockResult(list);

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
    assertTrue(resultBody.contains("java.lang.NullPointerException"));
    resultEndpoint.assertIsSatisfied();

    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
    assertTrue(respOutLogMsg.contains("Payload=java.lang.NullPointerException"));
  }

  @Test // If a producer sends soap fault, we shall return with ResponseCode 500, with the fault embedded in the body.
  public void soapFaultPropagatedToCustomerTest() throws InterruptedException {
    mockProducer.setResponseHttpStatus(500);
    mockProducer.setResponseBody(SoapFaultHelper.generateSoap11FaultWithCause(REMOTE_SOAP_FAULT, VPFaultCodeEnum.Client));
 
    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(MOCK_PRODUCER_ADDRESS, RIV20));
    setTakCacheMockResult(list);
    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));

    String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
    assertTrue(resultBody.contains("<faultcode>soapenv:Client</faultcode>"));
    assertTrue(resultBody.contains("VP011"));
    assertTrue(resultBody.contains("Anrop har gjorts"));
    assertTrue(resultBody.contains("Caller was not on the white list of accepted IP-addresses"));
    resultEndpoint.assertIsSatisfied();
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));

    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
    assertTrue(respOutLogMsg.contains("CamelHttpResponseCode=500"));
    assertTrue(respOutLogMsg.contains("Internal Server Error"));
    assertTrue(respOutLogMsg.contains("Payload=<soapenv:Envelope"));
    assertTrue(respOutLogMsg.contains("VP011"));
    assertTrue(respOutLogMsg.contains("Anrop har gjorts"));
  }

  private void setTakCacheMockResult(List<RoutingInfo> list) {
    Mockito.when(takCache.getRoutingInfo("urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", "UnitTest"))
        .thenReturn(list);
    Mockito
        .when(takCache.isAuthorized("UnitTest", "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", "UnitTest"))
        .thenReturn(true);
  }


  private void addConsumerRoute(CamelContext camelContext) throws Exception {
    camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start").routeDescription("Consumer").id("Consumer")
            .setHeader(HttpHeaders.X_VP_SENDER_ID, constant("UnitTest"))
            .setHeader(HttpHeaders.X_VP_INSTANCE_ID, constant("dev_env"))
            .setHeader("X-Forwarded-For", constant("1.2.3.4"))
            .to("netty-http:"+VP_ADDRESS+"?throwExceptionOnFailure=false")
            .to("mock:result");
      }
    });
  }

  private String createExceptionMessage(){
    try {
      String.valueOf(null);
    } catch (NullPointerException e) {
      return(e.toString());
    }
    return "Should not happen";
  }

}