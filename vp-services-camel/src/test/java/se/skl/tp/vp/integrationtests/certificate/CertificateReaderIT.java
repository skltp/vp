package se.skl.tp.vp.integrationtests.certificate;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.NettyConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.certificate.CertificateExtractorProcessor;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

@CamelSpringBootTest
@ContextConfiguration(classes = TestBeanConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class  CertificateReaderIT  extends CamelTestSupport {

  @Autowired
  BuildProperties buildProperties;

  @EndpointInject("mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce("direct:start")
  protected ProducerTemplate template;

  @Autowired
  CertificateExtractorProcessor certificateExtractorProcessor;

  @BeforeAll
  public static void startLeakDetection() {
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterAll
  public static void verifyNoLeaks() throws Exception {
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  private final String certificateSubject =
          "CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB";

  @Test
  public void extractSenderIdFromCertHeader() throws Exception {
    String expectedBody = "test";

    resultEndpoint.expectedBodiesReceived(expectedBody);
    resultEndpoint.expectedPropertyReceived(VPExchangeProperties.SENDER_ID, "Harmony");

    template.sendBody(expectedBody);
    resultEndpoint.assertIsSatisfied();
  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:start")
            .to("netty-http:http://localhost:12123/vp");

        from("netty-http:http://localhost:12123/vp")
            .process((Exchange exchange) -> {
              exchange.getIn().setHeader(NettyConstants.NETTY_SSL_CLIENT_CERT_SUBJECT_NAME, certificateSubject);
              certificateExtractorProcessor.process(exchange);
            })
            .to("mock:result");
      }
    };
  }
}
