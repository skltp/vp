package se.skl.tp.vp.integrationtests.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.VPRouter.VP_HTTPS_ROUTE;
import static se.skl.tp.vp.VPRouter.VP_HTTP_ROUTE;
import static se.skl.tp.vp.integrationtests.utils.RouteProcessorByClassHelper.processorOfClassExistsInRoute;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.certificate.CertificateExtractorProcessor;
import se.skl.tp.vp.httpheader.HttpSenderIdExtractorProcessor;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class VPRouterIT {
  @Autowired private CamelContext camelContext;

  /**
   * Test class for examining aspects of different routes set up by VPRouter, is on pair with
   * intended function.
   */
  public VPRouterIT(){

  }

  @BeforeEach
  public void setUp() {}

  @Test
  public void testCertificateExtractorProcessorExistsInHttpsRoute() {
    assertTrue(
    		processorOfClassExistsInRoute(
    	        camelContext.getRoute(VP_HTTPS_ROUTE), CertificateExtractorProcessor.class),
    			"The HTTPS route must contain CertificateExtractorProcessor"
        );
  }

  @Test
  public void testHttpSenderIdExtractorProcessorNotExistsInHttpsRoute() {
    assertFalse(
    	processorOfClassExistsInRoute(
    	        camelContext.getRoute(VP_HTTPS_ROUTE), HttpSenderIdExtractorProcessor.class),
        "The HTTPS route must not contain HttpSenderIdExtractorProcessor"
        );
  }

  @Test
  public void testHttpSenderIdExtractorProcessorExistInHttpRoute() {
    assertTrue(
            processorOfClassExistsInRoute(
                    camelContext.getRoute(VP_HTTP_ROUTE), HttpSenderIdExtractorProcessor.class),
            "The HTTP route must contain HttpSenderIdExtractorProcessor"
    		);
  }

  @Test
  public void testCertificateExtractorProcessorNotExistInHttpRoute() {
    assertFalse(
            processorOfClassExistsInRoute(
                    camelContext.getRoute(VP_HTTP_ROUTE), CertificateExtractorProcessor.class),
            "The HTTP route must NOT contain CertificateExtractorProcessor"
);
  }

}
