package se.skl.tp.vp.integrationtests.errorhandling;

import static org.apache.camel.test.junit4.TestSupport.assertStringContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.skl.tp.vp.VPRouter.VAGVAL_PROCESSOR_ID;
import static se.skl.tp.vp.VPRouter.VAGVAL_ROUTE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.Map;
import javax.xml.soap.SOAPBody;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class SoapFaultIT extends LeakDetectionBaseTest {

  public static final String TEST_EXCEPTION_MESSAGE = "Test exception message!";
  @EndpointInject(uri = "mock:vagvalprocessor")
  protected MockEndpoint resultEndpoint;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  private CamelContext camelContext;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @Before
  public void mockVagvalProcessor() throws Exception {
    replaceVagvalProcessor();
    makeMockVagvalProcessorThrowException();
    camelContext.start();
  }

  @Test
  public void unexpectedExceptionInRouteShouldResultInSoapFault() throws Exception {

    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull("Expected a SOAP message", soapBody);
    assertNotNull("Expected a SOAPFault", soapBody.hasFault());

    assertStringContains(soapBody.getFault().getFaultString(), TEST_EXCEPTION_MESSAGE);
    assertNumLogMessages();
    assertCorrelationIdIsSameInAllLogs();
  }

  private void assertNumLogMessages() {
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
    assertEquals(1, testLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
  }

  private void assertCorrelationIdIsSameInAllLogs() {
    // This check is done in old VP, VpFullServiceTest.testWhenErrorOneInfoEventAndOneErrorEventIsCreated(). Needed??
    String errMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_ERROR, 0);
    String reqInMsg = testLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    String respOutMsg = testLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    String corr1 = errMsg.substring(errMsg.indexOf("BusinessCorrelationId"), errMsg.indexOf("ExtraInfo")).trim();
    String corr2 = reqInMsg.substring(reqInMsg.indexOf("BusinessCorrelationId"), reqInMsg.indexOf("ExtraInfo")).trim();
    String corr3 = respOutMsg.substring(respOutMsg.indexOf("BusinessCorrelationId"), respOutMsg.indexOf("ExtraInfo")).trim();
    assertEquals(corr1, corr2);
    assertEquals(corr2, corr3);
  }

  private void replaceVagvalProcessor() throws Exception {
    AdviceWithRouteBuilder mockNetty = new AdviceWithRouteBuilder() {

      @Override
      public void configure() throws Exception {
        weaveById(VAGVAL_PROCESSOR_ID)
            .replace().to("mock:vagvalprocessor");
      }
    };
    
    //camelContext.getRouteDefinition(VAGVAL_ROUTE).adviceWith(camelContext, mockNetty);
  }

  private void makeMockVagvalProcessorThrowException() {
    resultEndpoint.whenAnyExchangeReceived(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        throw new NullPointerException(TEST_EXCEPTION_MESSAGE);
      }
    });
  }

}
