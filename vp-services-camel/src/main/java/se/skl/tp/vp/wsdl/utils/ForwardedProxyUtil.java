package se.skl.tp.vp.wsdl.utils;

import java.net.MalformedURLException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;

@Service
public class ForwardedProxyUtil {

  @Autowired
  private ProxyHttpForwardedHeaderProperties proxyHttpForwardedHeaderProperties;

  public ProxyUrl getForwardProxyUrl(Exchange exchange) throws MalformedURLException {
    Message message = exchange.getIn();
    String protocol = (String) message.getHeader(proxyHttpForwardedHeaderProperties.getProto());
    String host = (String) message.getHeader(proxyHttpForwardedHeaderProperties.getHost());
    String port = (String) message.getHeader(proxyHttpForwardedHeaderProperties.getPort());
    return new ProxyUrl(protocol, host, port);
  }

  @Data
  @AllArgsConstructor
  @ToString
  public class ProxyUrl {
    String protocol;
    String host;
    String port;
  }

}
