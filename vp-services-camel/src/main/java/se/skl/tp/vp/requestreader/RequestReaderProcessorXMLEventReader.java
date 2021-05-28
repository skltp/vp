package se.skl.tp.vp.requestreader;

import static org.apache.commons.lang.CharEncoding.UTF_8;
import static se.skl.tp.vp.constants.HttpHeaders.SOAP_ACTION;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.converter.jaxp.StaxConverter;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.requestreader.PayloadInfoParser.PayloadInfo;

@Service
@Log4j2
public class RequestReaderProcessorXMLEventReader implements RequestReaderProcessor {

  public static final String RIVTABP_21 = "rivtabp21";
  public static final String RIVTABP_20 = "rivtabp20";
  public static final String MTOM_XML_EXTRACT_REGEX = "(<\\w+:Envelope.*<\\/\\w+:Envelope>)";
  Pattern pattern = Pattern.compile(MTOM_XML_EXTRACT_REGEX, Pattern.CASE_INSENSITIVE+Pattern.DOTALL);


  @Override
  public void process(Exchange exchange) throws Exception {
    try {
      handleMTOMMessage(exchange);
      XMLStreamReader reader = toStreamReader(exchange);
      PayloadInfo payloadInfo = PayloadInfoParser.extractInfoFromPayload(reader);

      exchange.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, payloadInfo.getServiceContractNamespace());
      exchange.setProperty(VPExchangeProperties.RECEIVER_ID, payloadInfo.getReceiverId());
      exchange.setProperty(VPExchangeProperties.RIV_VERSION, payloadInfo.getRivVersion());
      exchange.setProperty(VPExchangeProperties.XML_REQUEST_ENCODING, payloadInfo.getEncoding());

    } catch (final XMLStreamException e) {
      String corrId = exchange.getProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, String.class);
      String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
      String soapAction = exchange.getIn().getHeader(SOAP_ACTION, String.class);
      String msg = String.format("Failed parsing payload.\nCorrelationId: %s\nContent-Type: %s\nSoapAction: %s",
          corrId, contentType, soapAction);
      log.error(msg, e);
      throw new VpTechnicalException(e);
    }
  }

  private void handleMTOMMessage(Exchange exchange) {

    String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
    if (contentType != null && contentType.toLowerCase().contains("application/xop+xml")) {
      String corrId = exchange.getProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, String.class);
      log.warn("MTOM/XOP not fully supported, a binary attachment could be missing. Content-Type: {}, CorrelationId: {}",
          contentType, corrId);
      String mtomPayload = exchange.getIn().getBody(String.class);
      exchange.getIn().setBody(extractXmlPayload(mtomPayload));
    }
  }

  protected String extractXmlPayload(String mtomPayload) {
    log.debug("Extracting xml from MTOM msg:\n{}", mtomPayload);
    Matcher matcher = pattern.matcher(mtomPayload);
    if (matcher.find()){
      String xmlPayload = matcher.group(1);
      log.debug("XML extracted from MTOM msg:\n{}", xmlPayload);
      return xmlPayload;
    } else {
      throw new VpTechnicalException(String.format("Failed to extract XML part from MTOM message"));
    }
  }

  private XMLStreamReader toStreamReader(Exchange exchange) throws XMLStreamException, IOException {
	InputStream body = exchange.getIn().getBody(InputStream.class);
    try {
    	return new StaxConverter().createXMLStreamReader(body, exchange);
    } catch (Exception e) {
    	// This will basically handle the case where encoding is UTF-8 but xml prolog is UTF-16
        log.warn("Failed convert payload to XMLStreamReader. Trying with default encoding UTF-8... " + e.getMessage());
        exchange.setProperty(Exchange.CHARSET_NAME, UTF_8);
        body.reset();
    	return new StaxConverter().createXMLStreamReader(body, exchange);
    }
  }
}
