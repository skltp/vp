package se.skl.tp.vp.charset;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;


@Service
public class ConvertResponseCharset implements Processor {

  static final String VARIABLE_CONTENT_TYPE = "text/xml; charset=%s";

  @Override
  public void process(Exchange exchange) throws Exception {
    String originalEncoding = exchange.getProperty(VPExchangeProperties.ORIGINAL_REQUEST_ENCODING, String.class);
    if(originalEncoding!=null && !originalEncoding.isEmpty()){
      convertPayloadToString(exchange);
      exchange.setProperty(Exchange.CHARSET_NAME, originalEncoding);
      exchange.getIn().setHeader(Exchange.CONTENT_TYPE, String.format(VARIABLE_CONTENT_TYPE, originalEncoding));
    } else {
      exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/xml; charset=UTF-8");
    }
  }

  private void convertPayloadToString(Exchange exchange) {
    Object body = exchange.getIn().getBody();
    if(body instanceof byte[]){
      exchange.getIn().setBody(exchange.getIn().getBody(String.class));
    }
  }


}
