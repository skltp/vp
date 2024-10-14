package se.skl.tp.vp.charset;

import static org.apache.commons.lang.CharEncoding.UTF_8;

import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;

@Service
@Log4j2
public class ConvertRequestCharset implements Processor {

  private static final String DEFAULT_ENCODING = UTF_8;

  @Override
  public void process(Exchange exchange) throws Exception {
    String xmlRequestEncoding = exchange.getProperty(VPExchangeProperties.XML_REQUEST_ENCODING, String.class);
    if (xmlRequestEncoding!=null && !xmlRequestEncoding.toUpperCase().startsWith(DEFAULT_ENCODING)) {
      convertBodyToUTF8String(exchange);
      exchange.setProperty(Exchange.CHARSET_NAME, DEFAULT_ENCODING);
      exchange.setProperty(VPExchangeProperties.ORIGINAL_REQUEST_ENCODING, xmlRequestEncoding);
    }
  }

  private void convertBodyToUTF8String(Exchange exchange) throws TransformerException {
    XMLStreamReader body = exchange.getIn().getBody(XMLStreamReader.class);
    StAXSource source = new StAXSource(body);

    StringWriter stringWriter = new StringWriter();
    StreamResult streamResult = new StreamResult(stringWriter);

    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    factory.newTransformer().transform(source, streamResult);

    exchange.getIn().setBody(stringWriter.getBuffer().toString());
  }


}
