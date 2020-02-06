package se.skl.tp.vp.camel;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.net.URI;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.netty4.http.DefaultNettyHttpBinding;
import org.apache.camel.component.netty4.http.NettyHttpConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;

@Component
@Log4j2
public class VPNettyHttpBinding extends DefaultNettyHttpBinding {

  @Value("${" + PropertyConstants.PRODUCER_CHUNKED_ENCODING + ":#{false}}")
  boolean useChunked;

  @Override
  public HttpRequest toNettyRequest(Message message, String uri, NettyHttpConfiguration configuration) throws Exception {
    // DefaultNettyHttpBinding will in some situations set port to -1 in the
    // in the HTTP "host" header. This will cause Apache proxy server to return a
    // HTTP 400 error. Therefor override DefaultNettyHttpBinding and change the
    // default "host header".
    // See ch. “14.23 Host”  in https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
    HttpRequest request = super.toNettyRequest(message, uri, configuration);
    URI u = new URI(uri);
    int port = u.getPort();
    String hostHeader = u.getHost() + (port == 80 || port == -1 ? "" : ":" + u.getPort());
    request.headers().set(HttpHeaderNames.HOST.toString(), hostHeader);
    if (useChunked) {
      request.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
      request.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
    }
    if(log.isDebugEnabled()){
      log.debug("Outgoing request headers:\n{}", request.headers().toString());
    }
    ThreadContext.clearAll();
    return request;
  }

  @Override
  public HttpResponse toNettyResponse(Message message, NettyHttpConfiguration configuration) throws Exception {
    HttpResponse response = super.toNettyResponse(message, configuration);
    if(log.isDebugEnabled()){
      log.debug("Outgoing response headers:\n{}", response.headers().toString());
    }
    ThreadContext.clearAll();
    return response;
  }

  @Override
  public Message toCamelMessage(FullHttpResponse response, Exchange exchange, NettyHttpConfiguration configuration)
      throws Exception {
    Message msg = super.toCamelMessage(response, exchange, configuration);
    String correlationId = exchange.getProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, String.class);
    ThreadContext.put("corr.id", String.format("[%s]", correlationId ));
    if(log.isDebugEnabled()){
      log.debug("Incomming response headers:\n{}", response.headers().toString());
    }
    return msg;
  }

  @Override
  public Message toCamelMessage(FullHttpRequest request, Exchange exchange, NettyHttpConfiguration configuration)
      throws Exception {
    Message msg = super.toCamelMessage(request, exchange, configuration);
    String correlationId = getCorrelationId(request);
    exchange.setProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, correlationId);
    ThreadContext.put("corr.id", String.format("[%s]", correlationId ));
    if(log.isDebugEnabled()){
      log.debug("Incomming request headers:\n{}", request.headers().toString());
    }
    return msg;
  }

  public String getCorrelationId(FullHttpRequest request) {

    if (request.headers().contains(HttpHeaders.X_SKLTP_CORRELATION_ID)) {
      String correlationId = request.headers().get(HttpHeaders.X_SKLTP_CORRELATION_ID);
      if (!StringUtils.isEmpty(correlationId)) {
        return correlationId;
      }
    }

    return UUID.randomUUID().toString();
   }

}
