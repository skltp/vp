package se.skl.tp.vp.errorhandling;

import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.apache.camel.Exchange;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VPFaultCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

public class SoapFaultHelper {

  private SoapFaultHelper(){}

  /*
   * Generic soap fault template, just use String.format(SOAP_FAULT, message);
   */
  private static final String SOAP_FAULT =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
          "  <soapenv:Header/>" +
          "  <soapenv:Body>" +
          "    <soapenv:Fault>" +
          "      <faultcode>soapenv:%s</faultcode>" +
          "      <faultstring>%s</faultstring>" +
          "      <detail></detail> " +
          "    </soapenv:Fault>" +
          "  </soapenv:Body>" +
          "</soapenv:Envelope>";


  public static String generateSoap11FaultWithCause(String cause, VPFaultCodeEnum codeEnum) {
    return String.format(SOAP_FAULT, codeEnum.getFaultCode(), escape(cause));
  }

  private static final String escape(final String string) {
    return StringEscapeUtils.escapeXml(string);
  }

  public static String getStatusMessage(String code, String defaultReason) {

    if (code == null || code.length() == 0) {
      return defaultReason;
    }

    try {
      Integer intCode = Integer.valueOf(code);
      String reason = HttpStatus.valueOf(intCode).getReasonPhrase();
      return code + " " + reason;
    } catch (Exception e) {
      return code;
    }

  }

  public static String nvl(Object s) {
    return (s == null) ? "" : s.toString();
  }

  private static Object createSoapFault(String faultMessage, String faultDetails, VpSemanticErrorCodeEnum errorCode) {
    try {
      MessageFactory messageFactory = MessageFactory.newInstance();
      SOAPMessage soapMessage = messageFactory.createMessage();
      SOAPFault soapFault = soapMessage.getSOAPBody().addFault();
      soapFault.setFaultCode(new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, errorCode.getFaultCode()));
      soapFault.setFaultString(faultMessage);
      Detail d = soapFault.addDetail();
      DetailEntry entry = d.addDetailEntry(new QName("detailString"));
      entry.addTextNode(faultDetails);
      return soapMessage.getSOAPPart();
    } catch (SOAPException e1) {
      return generateSoap11FaultWithCause(faultMessage, VPFaultCodeEnum.Server);
    }
  }

  public static void setSoapFaultInResponse(Exchange exchange, String faultString, String faultDetails, VpSemanticErrorCodeEnum errorCode){
    exchange.getMessage().setBody(createSoapFault(faultString, faultDetails, errorCode));
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
    exchange.setProperty(VPExchangeProperties.SESSION_ERROR, Boolean.TRUE);
    exchange.setProperty(VPExchangeProperties.SESSION_ERROR_CODE, errorCode.getVpDigitErrorCode());
    exchange.setProperty(VPExchangeProperties.SESSION_HTML_STATUS, SoapFaultHelper.getStatusMessage(nvl(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE)), null));
  }
}
