package se.skl.tp.vp.certificate;

import java.net.InetSocketAddress;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;
import se.skl.tp.vp.httpheader.SenderIpExtractor;
import se.skl.tp.vp.httpheader.SenderIpExtractorFromHeader;

public class SenderIpExtractorTest {

  @Test
  public void extractIPFromNettyHeader() throws Exception {
    SenderIpExtractor senderIpExtractor = new SenderIpExtractorFromHeader("X-Forwarded-For");

    Message message = createMessage();
    String SenderIpAddess = senderIpExtractor.getSenderIpAdress(message);
    Assert.assertEquals("10.11.12.13", SenderIpAddess);
  }

  @Test
  public void extractIPFromForwardedHeader() throws Exception {
   SenderIpExtractor senderIpExtractor = new SenderIpExtractorFromHeader("X-Forwarded-For");

    Message message = createMessage();
    message.setHeader("X-Forwarded-For", "13.12.10.11");
    String SenderIpAddess = senderIpExtractor.getSenderIpAdress(message);
    Assert.assertEquals("13.12.10.11", SenderIpAddess);
  }

  private Message createMessage() {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    InetSocketAddress inetSocketAddress = new InetSocketAddress("10.11.12.13",8585);
    ex.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, inetSocketAddress);
    return ex.getIn();
  }

}
