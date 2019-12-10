package se.skl.tp.vp.integrationtests.utils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Message;
import org.apache.camel.component.netty4.http.DefaultNettyHttpBinding;
import org.apache.camel.component.netty4.http.NettyHttpConfiguration;
import org.springframework.stereotype.Component;

@Component
@Data
@Log4j2
public class ProducerContentLengthManipulator extends DefaultNettyHttpBinding {

  protected int contentLengthAdder = 5;

  @Override
  public HttpResponse toNettyResponse(Message message, NettyHttpConfiguration configuration) throws Exception {
    HttpResponse httpResponse = super.toNettyResponse(message,configuration);
    int len = httpResponse.headers().getInt(HttpHeaderNames.CONTENT_LENGTH.toString());
    log.warn("Content-Lenght: {} will be manipulated with: {}",len,contentLengthAdder);
    httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH.toString(), len+contentLengthAdder);
    return httpResponse;
  }

}
