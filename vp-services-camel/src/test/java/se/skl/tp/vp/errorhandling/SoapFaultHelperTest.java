package se.skl.tp.vp.errorhandling;

import static org.junit.Assert.*;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.Test;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

public class SoapFaultHelperTest {

  @Test
  public void setSoapFaultInResponse() {
    Exchange exchange = createExchange();
    SoapFaultHelper.setSoapFaultInResponse(exchange, "Something wrong", VpSemanticErrorCodeEnum.VP009.toString());
    String body = exchange.getOut().getBody(String.class);
    assertTrue( body, body.contains("http://schemas.xmlsoap.org/soap/envelope/"));
    assertTrue( body, body.contains("Something wrong"));
    assertTrue((int)exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE)==500);
    assertTrue((Boolean)exchange.getProperty(VPExchangeProperties.SESSION_ERROR));
    assertTrue(exchange.getProperty(VPExchangeProperties.SESSION_ERROR_CODE).equals(VpSemanticErrorCodeEnum.VP009.toString()));

  }
  private Exchange createExchange() {
    CamelContext ctx = new DefaultCamelContext();
    return new DefaultExchange(ctx);
  }
}