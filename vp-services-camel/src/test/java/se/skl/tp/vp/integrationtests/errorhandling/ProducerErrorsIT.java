package se.skl.tp.vp.integrationtests.errorhandling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.VPRouter.*;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.*;

import jakarta.xml.soap.SOAPBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

import javax.net.ssl.SSLHandshakeException;

@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
public class ProducerErrorsIT extends LeakDetectionBaseTest {

  public static final String TEST_EXCEPTION_MESSAGE = "Test exception message!";
  @EndpointInject("mock:producer")
  protected MockEndpoint resultEndpoint;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  private CamelContext camelContext;

  @BeforeEach
  public void mockProducerRoute() throws Exception {
    addMockProducer();
  }

  @Test
  public void sslHandshakeExceptionInRouteShouldResultInVP009SoapFault() throws Exception {
    makeMockProducerThrowException(new SSLHandshakeException(TEST_EXCEPTION_MESSAGE));
    camelContext.start();

    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull(soapBody, "Expected a SOAP message");
    assertTrue(soapBody.hasFault(), "Expected a SOAPFault");
    assertStringContains(soapBody.getFault().getFaultString(), "VP009");
    assertStringContains(soapBody.getFault().getDetail().getTextContent(), TEST_EXCEPTION_MESSAGE);
  }

  @Test
  public void ioExceptionInRouteShouldResultInVP009SoapFault() throws Exception {
    makeMockProducerThrowException(new IOException(TEST_EXCEPTION_MESSAGE));
    camelContext.start();

    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull(soapBody, "Expected a SOAP message");
    assertTrue(soapBody.hasFault(), "Expected a SOAPFault");
    assertStringContains(soapBody.getFault().getFaultString(), "VP009");
    assertStringContains(soapBody.getFault().getDetail().getTextContent(), TEST_EXCEPTION_MESSAGE);
  }

  private void addMockProducer() throws Exception {
	  AdviceWith.adviceWith(camelContext, TO_PRODUCER_ROUTE, a -> {
          a.interceptSendToEndpoint(".*localhost.*")
                  .skipSendToOriginalEndpoint()
                  .to("mock:producer");
		}
	  );
  }

  private void makeMockProducerThrowException(Exception e) {
    resultEndpoint.whenAnyExchangeReceived(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        throw e;
      }
    });
  }

}
