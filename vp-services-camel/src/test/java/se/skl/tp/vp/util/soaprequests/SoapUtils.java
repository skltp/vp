package se.skl.tp.vp.util.soaprequests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

public class SoapUtils {

  private SoapUtils() {
    // Utils class
  }

  public static SOAPMessage getSoapMessage(String soapMessage){
    try {
      return MessageFactory
          .newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(new MimeHeaders(),  new ByteArrayInputStream(soapMessage.getBytes()));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SOAPException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static SOAPBody getSoapBody(String soapMessageString){
    SOAPMessage soapMessage = getSoapMessage(soapMessageString);
    try {
      return soapMessage == null ? null : soapMessage.getSOAPBody();
    } catch (SOAPException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static SOAPFault getSoapFault(String soapMessageString){
    SOAPBody soapBody = getSoapBody(soapMessageString);
    return soapBody == null ? null : soapBody.getFault();
  }


}
