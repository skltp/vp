package se.skl.tp.vp.integrationtests.errorhandling;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.VPRouter.VAGVAL_PROCESSOR_ID;
import static se.skl.tp.vp.VPRouter.VAGVAL_ROUTE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.util.regex.Pattern;

import jakarta.xml.soap.SOAPBody;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
public class SoapFaultIT extends LeakDetectionBaseTest {

  public static final String TEST_EXCEPTION_MESSAGE = "Test exception message!";
  @EndpointInject("mock:vagvalprocessor")
  protected MockEndpoint resultEndpoint;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  private CamelContext camelContext;

  @BeforeEach
  void mockVagvalProcessor() throws Exception {
    replaceVagvalProcessor();
    makeMockVagvalProcessorThrowException();
    camelContext.start();
  }

  @Test
  void unexpectedExceptionInRouteShouldResultInSoapFault() {

    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), java.util.Collections.emptyMap());

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull(soapBody, "Expected a SOAP message");
    assertTrue(soapBody.hasFault(), "Expected a SOAPFault");

    String faultString = soapBody.getFault().getFaultString();
    assertStringContains(faultString, TEST_EXCEPTION_MESSAGE);
    assertNumLogMessages();
    assertCorrelationIdIsSameInAllLogs();
  }

  private void assertNumLogMessages() {
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_IN));
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
  }

  private void assertCorrelationIdIsSameInAllLogs() {
    // This check is done in old VP, VpFullServiceTest.testWhenErrorOneInfoEventAndOneErrorEventIsCreated(). Needed??
    String errMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_ERROR, 0);
    String reqInMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_IN, 0);
    String respOutMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT, 0);
    assertNotNull(errMsg);
    assertNotNull(reqInMsg);
    assertNotNull(respOutMsg);
    var regex = Pattern.compile("trace.id=\"([^\"]+)\"");
    var errMatcher = regex.matcher(errMsg);
    var reqInMatcher = regex.matcher(reqInMsg);
    var respOutMatcher = regex.matcher(respOutMsg);
    assertTrue(errMatcher.find(), "No trace.id found in REQ_ERROR log");
    assertTrue(reqInMatcher.find(), "No trace.id found in REQ_IN log");
    assertTrue(respOutMatcher.find(), "No trace.id found in RESP_OUT log");
    String errTraceId = errMatcher.group(1);
    String reqInTraceId = reqInMatcher.group(1);
    String respOutTraceId = respOutMatcher.group(1);
    assertEquals(errTraceId, reqInTraceId);
    assertEquals(reqInTraceId, respOutTraceId);
  }

  private void replaceVagvalProcessor() throws Exception {

	  AdviceWith.adviceWith(
        camelContext, VAGVAL_ROUTE,
        a -> a.weaveById(VAGVAL_PROCESSOR_ID).replace().to("mock:vagvalprocessor")
    );
    
  }

  private void makeMockVagvalProcessorThrowException() {
    resultEndpoint.whenAnyExchangeReceived(exchange -> {
      throw new NullPointerException(TEST_EXCEPTION_MESSAGE);
    });
  }
}
