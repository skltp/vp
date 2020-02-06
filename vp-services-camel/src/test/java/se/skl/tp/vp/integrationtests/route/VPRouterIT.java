package se.skl.tp.vp.integrationtests.route;

import static org.junit.Assert.*;
import static se.skl.tp.vp.VPRouter.VP_HTTPS_ROUTE;
import static se.skl.tp.vp.VPRouter.VP_HTTP_ROUTE;
import static se.skl.tp.vp.integrationtests.utils.RouteProcessorByClassHelper.processorOfClassExistsInRoute;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.certificate.CertificateExtractorProcessor;
import se.skl.tp.vp.httpheader.HttpSenderIdExtractorProcessor;

@RunWith(CamelSpringBootRunner.class)
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

  @Before
  public void setUp() {}

  @Test
  public void testCertificateExtractorProcessorExistsInHttpsRoute() {
    assertTrue(
        "The HTTPS route must contain CertificateExtractorProcessor",
        processorOfClassExistsInRoute(
            camelContext.getRoute(VP_HTTPS_ROUTE), CertificateExtractorProcessor.class));
  }

  @Test
  public void testHttpSenderIdExtractorProcessorNotExistsInHttpsRoute() {
    assertFalse(
        "The HTTPS route must not contain HttpSenderIdExtractorProcessor",
        processorOfClassExistsInRoute(
            camelContext.getRoute(VP_HTTPS_ROUTE), HttpSenderIdExtractorProcessor.class));
  }

  @Test
  public void testHttpSenderIdExtractorProcessorExistInHttpRoute() {
    assertTrue(
        "The HTTP route must contain HttpSenderIdExtractorProcessor",
        processorOfClassExistsInRoute(
            camelContext.getRoute(VP_HTTP_ROUTE), HttpSenderIdExtractorProcessor.class));
  }

  @Test
  public void testCertificateExtractorProcessorNotExistInHttpRoute() {
    assertFalse(
        "The HTTP route must NOT contain CertificateExtractorProcessor",
        processorOfClassExistsInRoute(
            camelContext.getRoute(VP_HTTP_ROUTE), CertificateExtractorProcessor.class));
  }

}
