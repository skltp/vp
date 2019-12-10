package se.skl.tp.vp.wsdl.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.constants.PropertyConstants;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProxyHttpForwardedHeaderProperties.class)
public class ForwardedHttpHeadersBaseUrlFactoryTest {

  private static String PORT = "443";
  private static String HOST = "vp-loadbalancer-dns-name";
  private static String SCHEME = "http";
  private static String QUERY = "xsd=../../core_components/itintegration_registry_1.0.xsd";

  private static String ORIGINAL_URL =
      "https://test.esb.ntjp.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl";

  private static String ORIGINAL_URL_WITH_FRAGMENT =
      "https://test.esb.ntjp.se/vp/clinicalprocess/healthcond/certificate/GetCertificate/2/rivtabp21?wsdl#fragment";

  @Value("${" + PropertyConstants.WSDLFILES_DIRECTORY + "}")
  private String wsdlDir;

  @Autowired private ProxyHttpForwardedHeaderProperties proxyHttpForwardedHeaderProperties;

  @Test
  public void extractForwardedHttpHeadersForBaseUrl()
      throws MalformedURLException, URISyntaxException {
    URL uri = new URL(ORIGINAL_URL);
//    BaseUrlModel baseUrlModel =
//        new ForwardedHttpHeadersBaseUrlFactory()
//            .extractForwardedHttpHeadersForBaseUrl(
//                createMessageWithForwardedHeader(), proxyHttpForwardedHeaderProperties, uri);
//    assertTrue(baseUrlModel.host.equals(HOST));
//    assertTrue(baseUrlModel.scheme.equals(SCHEME));
//    assertTrue(baseUrlModel.port.equals(PORT));
  }

  @Test
  public void extractFromOriginalUrlInstead() throws MalformedURLException, URISyntaxException {
//    URL uri = new URL(ORIGINAL_URL);
//    BaseUrlModel baseUrlModel =
//        new ForwardedHttpHeadersBaseUrlFactory()
//            .extractForwardedHttpHeadersForBaseUrl(createMessage(), proxyHttpForwardedHeaderProperties, uri);
//    assertTrue(baseUrlModel.host.equals("test.esb.ntjp.se"));
//    assertTrue(baseUrlModel.scheme.equals("https"));
//    assertTrue(baseUrlModel.port.equals("-1"));
  }

  @Test
  public void expandByForwardedHeaders() throws MalformedURLException, URISyntaxException {
//    URL uri = new URL(ORIGINAL_URL);
//    BaseUrlModel baseUrlModel =
//        new ForwardedHttpHeadersBaseUrlFactory()
//            .extractForwardedHttpHeadersForBaseUrl(
//                createMessageWithForwardedHeader(), proxyHttpForwardedHeaderProperties, uri);
//    Uri expandedUri =
//        new Uri(
//            new WsdlSchemaImportNodeHandler(null, null)
//                .replaceBaseUrlParts(uri, baseUrlModel, QUERY));
//
//    assertTrue(
//        expandedUri.getHost().equals(HOST)
//            && (expandedUri.getPort() == Integer.valueOf(PORT))
//            && expandedUri.getScheme().equals(SCHEME)
//            && expandedUri.getQuery().equals("?" + QUERY));
  }

  @Test
  public void expandByForwardedHeadersInkFragment()
      throws MalformedURLException, URISyntaxException {
//    URL uri = new URL(ORIGINAL_URL_WITH_FRAGMENT);
//    BaseUrlModel baseUrlModel =
//        new ForwardedHttpHeadersBaseUrlFactory()
//            .extractForwardedHttpHeadersForBaseUrl(
//                createMessageWithForwardedHeader(), proxyHttpForwardedHeaderProperties, uri);
//    URL expandedUri =
//        new URL(
//            new WsdlSchemaImportNodeHandler(null, null)
//                .replaceBaseUrlParts(uri, baseUrlModel, QUERY));
//
//    assertTrue(
//        expandedUri.getHost().equals(HOST)
//            && (expandedUri.getPort() == Integer.valueOf(PORT))
//            && expandedUri.toURI().getScheme().equals(SCHEME)
//            && expandedUri.getQuery().equals(QUERY)
//            && expandedUri.toURI().getFragment().equals("fragment"));
  }

  private Message createMessageWithForwardedHeader() {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getPort(), PORT);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getHost(), HOST);
    ex.getIn().setHeader(proxyHttpForwardedHeaderProperties.getProto(), SCHEME);
    return ex.getIn();
  }

  private Message createMessage() {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    return ex.getIn();
  }









}
