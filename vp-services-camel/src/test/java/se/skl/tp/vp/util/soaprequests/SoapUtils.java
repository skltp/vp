package se.skl.tp.vp.util.soaprequests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;

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
