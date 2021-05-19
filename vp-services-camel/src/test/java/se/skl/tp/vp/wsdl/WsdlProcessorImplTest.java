package se.skl.tp.vp.wsdl;

import static org.apache.camel.test.junit4.TestSupport.assertStringContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.skl.tp.vp.xmlutil.XmlHelper.selectXPathStringValue;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.wsdl.utils.ForwardedProxyUtil;


@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = {WsdlProcessorImpl.class, ProxyHttpForwardedHeaderProperties.class, ForwardedProxyUtil.class, WsdlConfigurationJson.class})
public class WsdlProcessorImplTest {

  private static String PORT = "443";
  private static String HOST = "vp-proxy-dns-name";
  private static String SCHEME = "https";

  private static String AUTOMATIC_DETECTED_WSDL_URL =
      "http://some.server.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl";
  private static String CONFIGURED_WSDL_URL ="http://0.0.0.0:8080/vp/SomWeirdUrlNotFollowingNamingConventions?wsdl";

  //The wsdl has a UTF8 boom (That shouldn't bee there)
  private static String CONFIGURED_WSDL_URL_UTF8_BOOM =
      "http://0.0.0.0:8080/vp/infrastructure/directory/authorizationmanagement/GetCredentialsForPerson/1/rivtabp21?wsdl";

  private static String XSD_REFERENCED_IN_WSDL = "http://some.server.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=GetCertificateResponder_2.1.xsd";
  private static String XSD_REFERENCED_IN_XSD = "http://some.server.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=GetCertificateResponder_2.1.xsd";

  @Autowired
  WsdlProcessor wsdlProcessor;

  @Autowired
  private ProxyHttpForwardedHeaderProperties proxyHttpForwardedHeaderProperties;

  @Test
  public void getWsdlAutomaticDetectedHappyDays() throws Exception {
    Exchange ex = createExchangeWithHttpUrl(AUTOMATIC_DETECTED_WSDL_URL);
    wsdlProcessor.process(ex);
    String body = ex.getIn().getBody(String.class);
    Document document = DocumentHelper.parseText(body);
    String name = selectXPathStringValue(document,"wsdl:definitions/@name","wsdl=http://schemas.xmlsoap.org/wsdl/");
    assertEquals("GetCertificateInteraction", name);
  }

  @Test
  public void getWsdlDefinedInConfigFileHappyDays() throws Exception {

    Exchange ex = createExchangeWithHttpUrl(CONFIGURED_WSDL_URL);
    wsdlProcessor.process(ex);
    Document document = DocumentHelper.parseText((String) ex.getIn().getBody());
    String name = selectXPathStringValue(document,"wsdl:definitions/@name","wsdl=http://schemas.xmlsoap.org/wsdl/");
    assertEquals("GetActivitiesInteraction", name);
  }

  @Test
  public void getWsdlDefinedInConfigFileHappyDaysUTF8BomIncluded() throws Exception {
    //Exchange ex = createExchangeWithHttpUrl(CONFIGURED_WSDL_URL);
    Exchange ex = createExchangeWithHttpUrl(CONFIGURED_WSDL_URL_UTF8_BOOM);
    wsdlProcessor.process(ex);
    Document document = DocumentHelper.parseText((String) ex.getIn().getBody());
    String name = selectXPathStringValue(document,"wsdl:definitions/@name","wsdl=http://schemas.xmlsoap.org/wsdl/");
    assertEquals("GetCredentialsForPersonInteraction", name);
    //assertEquals("GetActivitiesInteraction", name);
  }

  @Test
  public void wsdlShouldHaveUpdatedUrls() throws Exception {
    Exchange ex = createExchangeWithHttpUrl(AUTOMATIC_DETECTED_WSDL_URL);
    wsdlProcessor.process(ex);
    String body = ex.getIn().getBody(String.class);
    assertStringContains(body, XSD_REFERENCED_IN_WSDL );
    assertStringContains(body, "http://some.server.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd");
  }

  @Test
  public void wsdlShouldHaveUpdatedUrlsToProxyServerIfProxyUsed() throws Exception {
    Exchange ex = createMessageWithForwardedHeader(AUTOMATIC_DETECTED_WSDL_URL);
    wsdlProcessor.process(ex);
    String body = ex.getIn().getBody(String.class);
    assertStringContains(body, "https://vp-proxy-dns-name:443/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=GetCertificateResponder_2.1.xsd");
    assertStringContains(body, "https://vp-proxy-dns-name:443/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/itintegration_registry_1.0.xsd");
    assertStringContains(body, "<soap:address location=\"https://vp-proxy-dns-name:443/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21\"/>");
  }

  @Test
  public void noMatchingWsdlShouldResultInListOfPossibleWsdlPaths() throws Exception {

    Exchange ex = createExchangeWithHttpUrl("http://0.0.0.0:8080/vp/MyUnmatchedUri?wsdl");
    wsdlProcessor.process(ex);
    String body = (String) ex.getIn().getBody();
    assertTrue(body, body.contains("No wsdl found on this path, following wsdl paths is available:"));
    assertTrue(body, body.contains("vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21"));
    assertEquals( "text/plain;UTF-8", ex.getIn().getHeader("Content-Type"));
  }

  @Test
  public void getXsdReferencedFromWsdlHappyDays() throws Exception {

    Exchange ex = createExchangeWithHttpUrl(XSD_REFERENCED_IN_WSDL);
    ex.getIn().setHeader("xsd", "GetCertificateResponder_2.1.xsd");
    wsdlProcessor.process(ex);

    String body = ex.getIn().getBody(String.class);
    assertStringContains(body, "http://some.server.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?xsd=../../core_components/clinicalprocess_healthcond_certificate_3.2.xsd");

    Document document = DocumentHelper.parseText(body);
    String targetNamespace = selectXPathStringValue(document,"schema/@targetNamespace");
    assertEquals("urn:riv:clinicalprocess:healthcond:certificate:GetCertificateResponder:2", targetNamespace);

  }

  @Test
  public void getXsdReferencedFromAnotherXsdHappyDays() throws Exception {

    Exchange ex = createExchangeWithHttpUrl(XSD_REFERENCED_IN_WSDL);
    ex.getIn().setHeader("xsd", "clinicalprocess_healthcond_certificate_types_3.2.xsd");
    wsdlProcessor.process(ex);

    Document document = DocumentHelper.parseText(ex.getIn().getBody(String.class));
    String targetNamespace = selectXPathStringValue(document,"schema/@targetNamespace");
    assertEquals("urn:riv:clinicalprocess:healthcond:certificate:types:3", targetNamespace);

  }


  private Exchange createExchangeWithHttpUrl(String httpUrl) throws MalformedURLException {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    URL url = new URL(httpUrl);
    ex.getIn().setHeader(Exchange.HTTP_URL, url);
    return ex;
  }

  private Exchange createMessageWithForwardedHeader(String httpUrl) throws MalformedURLException {
    Exchange ex = createExchangeWithHttpUrl(httpUrl);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getPort(), PORT);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getHost(), HOST);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getProto(), SCHEME);
    return ex;
  }

  private Message createMessage() {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    return ex.getIn();
  }
}
