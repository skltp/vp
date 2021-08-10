package se.skl.tp.vp.httpheader;

import org.apache.camel.Message;
import org.apache.camel.component.netty.NettyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.PropertyConstants;

import java.net.InetSocketAddress;

@Service
public class SenderIpExtractorFromHeader implements SenderIpExtractor {

  private final String forwardProxyRemoteIpHeaderName;

  @Autowired
  public SenderIpExtractorFromHeader(
      @Value("${" + PropertyConstants.VAGVALROUTER_SENDER_IP_ADRESS_HTTP_HEADER + "}") String forwardProxyRemoteIpHeaderName) {
    this.forwardProxyRemoteIpHeaderName = forwardProxyRemoteIpHeaderName;
  }

  @Override
  public String getSenderIpAdress(Message message) {
    return isProxyUsed(message) ? getForwardedForAddress(message) : getCallerRemoteAddress(message);
  }

  @Override
  public String getForwardedForAddress(Message message) {
    return message.getHeader(forwardProxyRemoteIpHeaderName, String.class);
  }

  @Override
  public String getCallerRemoteAddress(Message message) {
    InetSocketAddress inetSocketAddress = message.getHeader(NettyConstants.NETTY_REMOTE_ADDRESS, InetSocketAddress.class);
    return inetSocketAddress==null ? null : inetSocketAddress.getAddress().getHostAddress();
  }

  @Override
  public String getForwardForHeaderName() {
    return forwardProxyRemoteIpHeaderName;
  }

  @Override
  public String getCallerRemoteAddressHeaderName() {
    return NettyConstants.NETTY_REMOTE_ADDRESS;
  }

  @Override
  public Boolean isProxyUsed(Message message) {
    return message.getHeaders().containsKey(forwardProxyRemoteIpHeaderName);
  }

}
