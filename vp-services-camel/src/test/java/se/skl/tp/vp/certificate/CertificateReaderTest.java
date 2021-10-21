package se.skl.tp.vp.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticException;

public class CertificateReaderTest {

  final String vpInstance = "NTjP Develop";

  @Test
  public void testSenderIdExtractedFromCertificateSubject() throws Exception {
    Exchange exchange = createExchange("SERIALNUMBER=SE5565594230-BCQ,CN=kentor.ntjp.sjunet.org,O=Inera AB,L=Stockholm,C=SE");
    CertificateExtractorProcessor certificateExtractorProcessor = new CertificateExtractorProcessorImpl("(?:2.5.4.5|SERIALNUMBER)=([^,]+)", vpInstance);
    certificateExtractorProcessor.process(exchange);

    assertEquals("SE5565594230-BCQ", exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }

  @Test
  public void testExtractIdFromMessageNotContainingCertHeader() throws Exception {

    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () ->  {

	    Exchange exchange = createExchangeWithoutNettyCert();
	
	    CertificateExtractorProcessor certificateExtractorProcessor = new CertificateExtractorProcessorImpl("(?:2.5.4.5|SERIALNUMBER)=([^,]+)", vpInstance);
	    certificateExtractorProcessor.process(exchange);
            });
    assertTrue(exception.getMessage().contains("VP002"));

  }

  @Test
  public void testExtractSenderIdInHexFormat() throws Exception {
    // TODO Hitta exempel på hur ett riktigt serialnumber i hex format ser ut.
    Exchange exchange = createExchange("2.5.4.5=#13145453544e4d54323332313030303135362d423032,CN=kentor.ntjp.sjunet.org,O=Inera AB,L=Stockholm,C=SE");
    CertificateExtractorProcessor certificateExtractorProcessor = new CertificateExtractorProcessorImpl("(?:2.5.4.5|SERIALNUMBER)=([^,]+)", vpInstance);
    certificateExtractorProcessor.process(exchange);

    assertEquals("TSTNMT2321000156-B02", exchange.getProperty(VPExchangeProperties.SENDER_ID));
  }


  @Test
  public void testSenderIdNotFoundShouldCauseVP002() throws Exception {

    Exception exception = assertThrows(
    		VpSemanticException.class, 
            () ->  {
	    Exchange exchange = createExchange("CN=kentor.ntjp.sjunet.org,O=Inera AB,L=Stockholm,C=SE");
	    CertificateExtractorProcessor certificateExtractorProcessor = new CertificateExtractorProcessorImpl("(?:2.5.4.5|SERIALNUMBER)=([^,]+)", vpInstance);
	    certificateExtractorProcessor.process(exchange);
    });
    assertTrue(exception.getMessage().contains("VP002"));
  }

  private Exchange createExchange(String header) {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    ex.getIn().setHeader(NettyConstants.NETTY_SSL_CLIENT_CERT_SUBJECT_NAME, header);
    return ex;
  }

  private Exchange createExchangeWithoutNettyCert() {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    ex.getIn().setHeader("MyCatsName", "Sally");
    return ex;
  }

}
