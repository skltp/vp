package se.skl.tp.vp.httpheader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.skl.tp.vp.certificate.HeaderCertificateHelperImpl;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.errorhandling.VpCodeMessages;
import se.skl.tp.vp.exceptions.VpSemanticException;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(
    classes = {
      SenderIpExtractorFromHeader.class,
      HeaderCertificateHelperImpl.class,
      IPWhitelistHandlerImpl.class,
      HttpSenderIdExtractorProcessorImpl.class,
      VpCodeMessages.class,
      ExceptionUtil.class
    })
public class HttpSenderIdExtractorProcessorImplTest {

  public static final String VP_INSTANCE_ID = "dev_env";
  public static final String RTP_INSTANCE_ID = "rtp_env";
  public static final String WHITELISTED_IP_ADDRESS = "1.2.3.4";
  public static final String NOT_WHITELISTED_IP_ADDRESS = "10.20.30.40";
  public static final String HEADER_SENDER_ID = "Sender1";
  public static final String CERT_SENDER_ID = "urken";

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Autowired HttpSenderIdExtractorProcessorImpl httpHeaderExtractorProcessor;

  @Test
  public void internalCallShouldSetSenderIdFromInHeader() throws Exception {
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void internalCallAndSenderNotWhitelistedShouldThrowVP011() throws Exception {
    thrown.expect(VpSemanticException.class);
    thrown.expectMessage(containsString("VP011"));

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(NOT_WHITELISTED_IP_ADDRESS));

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void internalCallThroughProxyShouldSetSenderIdFromInHeader() throws Exception {
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", WHITELISTED_IP_ADDRESS);

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void internalCallThroughProxyAndSenderNotWhitelistedShouldThrowVP011() throws Exception {
    thrown.expect(VpSemanticException.class);
    thrown.expectMessage(containsString("VP011"));

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void nonInternalCallShouldSetSenderIdFromCertificate() throws Exception {
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(HttpHeaders.CERTIFICATE_FROM_REVERSE_PROXY, createMockCertificate());
    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(CERT_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }
  @Test
  public void nonInternalCallAndSenderNotWhitelistedShouldThrowVP011() throws Exception {
    thrown.expect(VpSemanticException.class);
    thrown.expectMessage(containsString("VP011"));

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(NOT_WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(HttpHeaders.CERTIFICATE_FROM_REVERSE_PROXY, createMockCertificate());
    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void ifAnotherInstanceSenderIdShouldBeExtractedFromCert() throws Exception {
    final X509Certificate cert = createMockCertificate();

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, RTP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(HttpHeaders.CERTIFICATE_FROM_REVERSE_PROXY, cert);

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(CERT_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test(expected = VpSemanticException.class)
  public void testExtractUnkownCertificateTypeFromHeader() throws Exception {
    final Certificate wrongTypecert = Mockito.mock(Certificate.class);
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, RTP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);
    exchange.getIn().setHeader(HttpHeaders.CERTIFICATE_FROM_REVERSE_PROXY, wrongTypecert);

    try {
      httpHeaderExtractorProcessor.process(exchange);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Exception, unkown certificate type found in httpheader"));
      throw e;
    }
  }

  private X509Certificate createMockCertificate() {
    final X500Principal principal = new X500Principal("OU=urken");
    final X509Certificate cert = Mockito.mock(X509Certificate.class);
    Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
    return cert;
  }

  private InetSocketAddress mockInetAddress(String address) {
    InetAddress inetAddress = Mockito.mock(InetAddress.class);
    Mockito.when(inetAddress.getHostAddress()).thenReturn(address);
    return new InetSocketAddress(inetAddress, 443);
  }

  private Exchange createExchange() {
    CamelContext ctx = new DefaultCamelContext();
    return new DefaultExchange(ctx);
  }
}
