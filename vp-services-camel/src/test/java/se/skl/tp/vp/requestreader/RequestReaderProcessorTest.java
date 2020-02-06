package se.skl.tp.vp.requestreader;

import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv20Request;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetActivitiesRiv21Request;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.VPExchangeProperties;

@RunWith(CamelSpringBootRunner.class)
@ContextConfiguration(classes = RequestReaderProcessorXMLEventReader.class)
@TestPropertySource("classpath:application.properties")
public class RequestReaderProcessorTest extends CamelTestSupport {

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Autowired
  RequestReaderProcessor requestReaderProcessor;

  @Test
  public void testExtractInfoFromMessageWithNSInEnvelope() throws Exception {
    resultEndpoint.expectedBodiesReceived(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RECEIVER_ID, "UnitTest");
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RIV_VERSION, RequestReaderProcessorXMLEventReader.RIVTABP_20);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");

    template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));
    resultEndpoint.assertIsSatisfied();

  }

  @Test
  public void testExtractInfoFromMessageWithNSInBody() throws Exception {
    resultEndpoint.expectedBodiesReceived(createGetActivitiesRiv21Request(RECEIVER_UNIT_TEST));
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RECEIVER_ID, "UnitTest");
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RIV_VERSION, RequestReaderProcessorXMLEventReader.RIVTABP_21);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv21Request(RECEIVER_UNIT_TEST));
    resultEndpoint.assertIsSatisfied();
  }

  @Test
  public void testExtractInfoFromMessageWithNSInContract() throws Exception {
    resultEndpoint.expectedBodiesReceived(createGetActivitiesRiv20Request(RECEIVER_UNIT_TEST));
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RECEIVER_ID, "UnitTest");
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RIV_VERSION, RequestReaderProcessorXMLEventReader.RIVTABP_20);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv20Request(RECEIVER_UNIT_TEST));
    resultEndpoint.assertIsSatisfied();
  }

  @Test
  public void testExtractInfoFromMessageWhereReceiverIsEmpty() throws Exception {
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv20Request(""));
    resultEndpoint.assertIsSatisfied();
    assertNull(resultEndpoint.getExchanges().get(0).getProperty(VPExchangeProperties.RECEIVER_ID));
  }


  @Test
  public void testExtractInfoFromMessageWhereReceiverIsJustSpaces() throws Exception {
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv20Request("   "));
    resultEndpoint.assertIsSatisfied();
    assertNull(resultEndpoint.getExchanges().get(0).getProperty(VPExchangeProperties.RECEIVER_ID));
  }

  @Test
  public void testExtractInfoFromMessageWhereReceiverStartsWithSpace() throws Exception {
    resultEndpoint.expectedBodiesReceived(createGetActivitiesRiv20Request(" UnitTest"));
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RECEIVER_ID, " UnitTest");
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RIV_VERSION, RequestReaderProcessorXMLEventReader.RIVTABP_20);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv20Request(" UnitTest"));
    resultEndpoint.assertIsSatisfied();
  }

  @Test
  public void testExtractInfoFromMessageWhereReceiverEndsWithSpace() throws Exception {
    resultEndpoint.expectedBodiesReceived(createGetActivitiesRiv20Request("UnitTest "));
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RECEIVER_ID, "UnitTest ");
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.RIV_VERSION, RequestReaderProcessorXMLEventReader.RIVTABP_20);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SERVICECONTRACT_NAMESPACE,
        "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1");

    template.sendBody(createGetActivitiesRiv20Request("UnitTest "));
    resultEndpoint.assertIsSatisfied();
  }


  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:start")
            .to("netty4-http:http://localhost:12123/vp");

        from("netty4-http:http://localhost:12123/vp")
            .process(requestReaderProcessor)
            .to("mock:result");
      }
    };
  }
}