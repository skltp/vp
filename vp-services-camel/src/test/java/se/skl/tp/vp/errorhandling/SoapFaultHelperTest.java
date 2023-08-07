package se.skl.tp.vp.errorhandling;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

public class SoapFaultHelperTest {

  @Test
  public void setSoapFaultInResponse() {
    Exchange exchange = createExchange();
    SoapFaultHelper.setSoapFaultInResponse(exchange, "Something wrong", "Fail details",VpSemanticErrorCodeEnum.VP009);
    String body = exchange.getMessage().getBody(String.class);
    assertTrue( body.contains("http://schemas.xmlsoap.org/soap/envelope/"), body);
    assertTrue( body.contains("Server"), body);
    assertTrue( body.contains("Something wrong"), body);
    assertEquals(500, (int) exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertTrue( body.contains("Fail details"), body);

    assertEquals(500, (int) exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertTrue((Boolean)exchange.getProperty(VPExchangeProperties.SESSION_ERROR));
    assertEquals(exchange.getProperty(VPExchangeProperties.SESSION_ERROR_CODE), VpSemanticErrorCodeEnum.VP009.toString());

  }


  @Test
  public void setSoapClientFaultInResponse() {
    Exchange exchange = createExchange();
    SoapFaultHelper.setSoapFaultInResponse(exchange, "Something wrong client","Fail details" ,VpSemanticErrorCodeEnum.VP001);
    String body = exchange.getMessage().getBody(String.class);
    assertTrue( body.contains("http://schemas.xmlsoap.org/soap/envelope/"), body);
    assertTrue( body.contains("Client"), body);
    assertTrue( body.contains("Something wrong"), body);
    assertTrue( body.contains("Fail details"), body);
    assertEquals(500, (int) exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertTrue((Boolean)exchange.getProperty(VPExchangeProperties.SESSION_ERROR));
    assertEquals(exchange.getProperty(VPExchangeProperties.SESSION_ERROR_CODE), VpSemanticErrorCodeEnum.VP001.toString());

  }
  private Exchange createExchange() {
    CamelContext ctx = new DefaultCamelContext();
    return new DefaultExchange(ctx);
  }
}