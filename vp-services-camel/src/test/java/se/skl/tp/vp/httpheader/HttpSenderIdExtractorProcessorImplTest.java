package se.skl.tp.vp.httpheader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import se.skl.tp.vp.certificate.HeaderCertificateHelperImpl;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.errorhandling.VpCodeMessages;
import se.skl.tp.vp.exceptions.VpSemanticException;

@CamelSpringBootTest
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
  public static final String NOTOK_ROUTING_HISTORY = "dev_env#some_server";
  public static final String OK_ROUTING_HISTORY = "some_server#some_other_server";

  @Value("${http.forwarded.header.auth_cert:X-VP-Auth-Cert}")
  String authCertHeaderName;

  @Autowired
  HttpSenderIdExtractorProcessorImpl httpHeaderExtractorProcessor;

  @Test
  public void internalCallShouldSetSenderIdFromInHeader() throws Exception {
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void internalCallAndSenderNotWhitelistedShouldThrowVP011() throws Exception {
    
    Exchange exchange = createExchange();

    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () -> {
    
			    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
			    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS,
        mockInetAddress(NOT_WHITELISTED_IP_ADDRESS));
			
			    httpHeaderExtractorProcessor.process(exchange);
            });

    assertTrue(exception.getMessage().contains("VP011"));
  }

  @Test
  public void routingHistoryShouldBeOk() throws Exception {

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, OK_ROUTING_HISTORY);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY), "some_server#some_other_server#dev_env");
    assertEquals(CERT_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void routingHistoryShouldBeConcatenated() throws Exception {

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, OK_ROUTING_HISTORY);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());

    httpHeaderExtractorProcessor.process(exchange);
    String result = exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, String.class);

    assertEquals(result, OK_ROUTING_HISTORY + "#" + VP_INSTANCE_ID);
  }

  @Test
  public void routingHistoryShouldContainSenderId() throws Exception {

    Exchange exchange = createExchange();
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());

    httpHeaderExtractorProcessor.process(exchange);
    String result = exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, String.class);

    assertTrue(result.contains(CERT_SENDER_ID));
  }

  @Test
  public void routingHistoryShouldContainSenderIdFromCertificateEvenWhenHeaderIsSet()
      throws Exception {

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());

    httpHeaderExtractorProcessor.process(exchange);
    String result = exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, String.class);

    assertTrue(result.contains(CERT_SENDER_ID));
  }

  @Test
  public void routingHistoryShouldThrowVP014() throws Exception {

    Exchange exchange = createExchange();
    
    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () -> {
  
			    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, NOTOK_ROUTING_HISTORY);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
			    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());
			
			    httpHeaderExtractorProcessor.process(exchange);
            });

    assertTrue(exception.getMessage().contains("VP014"));
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

    Exchange exchange = createExchange();
    
    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () -> {
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);

    httpHeaderExtractorProcessor.process(exchange);
            });

    assertTrue(exception.getMessage().contains("VP011"));
    
    //assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void nonInternalCallShouldSetSenderIdFromCertificate() throws Exception {
    Exchange exchange = createExchange();
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());
    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(CERT_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void nonInternalCallAndSenderNotWhitelistedShouldThrowVP011() throws Exception {
    Exchange exchange = createExchange();
    
    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () -> {
        exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS,
                mockInetAddress(NOT_WHITELISTED_IP_ADDRESS));
	    exchange.getIn().setHeader(authCertHeaderName, createMockCertificate());
	    httpHeaderExtractorProcessor.process(exchange);
            });

    assertTrue(exception.getMessage().contains("VP011"));
	 
    // Code never calls this in junit4 so I have commmented it out
    //assertEquals(HEADER_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }


  @Test
  public void ifAnotherInstanceSenderIdShouldBeExtractedFromCert() throws Exception {
    final X509Certificate cert = createMockCertificate();

    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, RTP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);
    exchange.getIn()
        .setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, mockInetAddress(WHITELISTED_IP_ADDRESS));
    exchange.getIn().setHeader(authCertHeaderName, cert);

    httpHeaderExtractorProcessor.process(exchange);

    assertEquals(CERT_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void testExtractUnkownCertificateTypeFromHeader() throws Exception {
    final Certificate wrongTypecert = Mockito.mock(Certificate.class);
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_VP_SENDER_ID, HEADER_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, RTP_INSTANCE_ID);
    exchange.getIn().setHeader("X-Forwarded-For", NOT_WHITELISTED_IP_ADDRESS);
    exchange.getIn().setHeader(authCertHeaderName, wrongTypecert);

    try {
      httpHeaderExtractorProcessor.process(exchange);
    } catch (VpSemanticException e) {
      assertTrue(e.getMessage().contains("VP002"));
      assertTrue(e.getMessage().contains("Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad"));
      assertTrue(e.getMessageDetails().contains("Exception, unkown certificate type found in httpheader"));
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
