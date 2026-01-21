package se.skl.tp.vp.util.soaprequests;

public class TestSoapRequests {

  private TestSoapRequests(){
    // Static utility to create Soap request
  }

  public static final String RECEIVER_UNIT_TEST = "UnitTest";
  public static final String RECEIVER_HTTP = "HttpProducer";
  public static final String RECEIVER_HTTPS = "HttpsProducer";
  public static final String RECEIVER_TRAILING_WHITESPACE = "HttpsProducer ";
  public static final String RECEIVER_LEADING_WHITESPACE = " HttpsProducer";
  public static final String RECEIVER_NO_PRODUCER_AVAILABLE = "ReceiverNoProducerAvailable";
  public static final String RECEIVER_WITH_NO_VAGVAL = "NoVagvalReceiver";
  public static final String RECEIVER_NOT_AUTHORIZED = "NotAuthorizedReceiver";
  public static final String RECEIVER_UNKNOWN_RIVVERSION = "ReceiverUnknownRivVersion";
  public static final String RECEIVER_MULTIPLE_VAGVAL = "ReceiverMultipleVagval";
  public static final String RECEIVER_NO_PHYSICAL_ADDRESS = "ReceiverNoPhysicalAddress";
  public static final String RECEIVER_RIV21 = "ReceiverRiv21";
  public static final String RECEIVER_RIV20 = "ReceiverRiv20";

  public static final String TJANSTEKONTRAKT_GET_CERTIFICATE_KEY = "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1";


  public static final String GET_CERTIFICATE_TO_UNIT_TEST_SOAP_REQUEST_VARIABLE_RECEIVER =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:add=\"http://www.w3.org/2005/08/addressing\" xmlns:urn=\"urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1\">\n"
          +
          "   <soapenv:Header>\n" +
          "      <add:To>%s</add:To>\n" +
          "   </soapenv:Header>\n" +
          "   <soapenv:Body>\n" +
          "      <urn:GetCertificateRequest>\n" +
          "         <urn:certificateId>?</urn:certificateId>\n" +
          "         <urn:nationalIdentityNumber>?</urn:nationalIdentityNumber>\n" +
          "         <!--You may enter ANY elements at this point-->\n" +
          "      </urn:GetCertificateRequest>\n" +
          "   </soapenv:Body>\n" +
          "</soapenv:Envelope>";

  public static final String GET_CERTIFICATE_REQUEST_RIV20_UTF16 =
      "<?xml version=\"1.0\" encoding=\"UTF-16\"?>" +
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:add=\"http://www.w3.org/2005/08/addressing\" xmlns:urn=\"urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1\">\n"
          +
          "   <soapenv:Header>\n" +
          "      <add:To>%s</add:To>\n" +
          "   </soapenv:Header>\n" +
          "   <soapenv:Body>\n" +
          "      <urn:GetCertificateRequest>\n" +
          "         <urn:certificateId>?</urn:certificateId>\n" +
          "         <urn:nationalIdentityNumber>?</urn:nationalIdentityNumber>\n" +
          "         <!--You may enter ANY elements at this point-->\n" +
          "      </urn:GetCertificateRequest>\n" +
          "   </soapenv:Body>\n" +
          "</soapenv:Envelope>";

  public static final String GET_ACTIVITIES_REQUEST_RIV21 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"  >\n"
          +
          "   <soapenv:Header xmlns:urn=\"urn:riv:itintegration:registry:1\">\n" +
          "      <urn:LogicalAddress>%s</urn:LogicalAddress>\n" +
          "   </soapenv:Header>\n" +
          "   <soapenv:Body xmlns:urn1=\"urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1\" xmlns:urn2=\"urn:riv:clinicalprocess:activity:actions:1\">\n" +
          "      <urn1:GetActivities>\n" +
          "         <urn1:patientId>\n" +
          "             <urn2:root>patientIdType</urn2:root>\n"+
          "             <urn2:extension>197404188888</urn2:extension>\n" +
          "         </urn1:patientId>\n" +
          "         <urn1:interactionAgreementId>2866a7c4-9c60-433f-9035-a4d779ffe7a1</urn1:interactionAgreementId>" +
          "         <urn1:sourceSystemId>"+
          "             <urn2:root>1.2.752.129.2.1.4.1</urn2:root>" +
          "             <urn2:extension>${sourceSystemHSAId}</urn2:extension>" +
          "         </urn1:sourceSystemId>" +
          "      </urn1:GetActivities>\n" +
          "   </soapenv:Body>\n" +
          "</soapenv:Envelope>";

  public static final String GET_ACTIVITIES_REQUEST_RIV20 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:add=\"http://www.w3.org/2005/08/addressing\" >\n"
          +
          "   <soapenv:Header>\n" +
          "      <add:To>%s</add:To>\n" +
          "   </soapenv:Header>\n" +
          "   <soapenv:Body>\n" +
          "      <GetActivities  xmlns=\"urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1\" xmlns:urn2=\"urn:riv:clinicalprocess:activity:actions:1\">\n" +
          "         <patientId>\n" +
          "             <urn2:root>patientIdType</urn2:root>\n"+
          "             <urn2:extension>197404188888</urn2:extension>\n" +
          "         </patientId>\n" +
          "         <interactionAgreementId>2866a7c4-9c60-433f-9035-a4d779ffe7a1</interactionAgreementId>" +
          "         <sourceSystemId>"+
          "             <urn2:root>1.2.752.129.2.1.4.1</urn2:root>" +
          "             <urn2:extension>${sourceSystemHSAId}</urn2:extension>" +
          "         </sourceSystemId>" +
          "      </GetActivities>\n" +
          "   </soapenv:Body>\n" +
          "</soapenv:Envelope>";

  public static final String GET_NO_CERT_HTTP_SOAP_REQUEST =  createGetCertificateRequest(RECEIVER_HTTP);

  public static final String GET_NO_CERT_HTTP_SOAP_REQUEST_NO_VAGVAL_RECEIVER =  createGetCertificateRequest(RECEIVER_WITH_NO_VAGVAL);

  public static final String GET_CERT_HTTPS_REQUEST = createGetCertificateRequest(RECEIVER_HTTPS);

  public static String createGetCertificateRequest(String receiver){
    return String.format(GET_CERTIFICATE_TO_UNIT_TEST_SOAP_REQUEST_VARIABLE_RECEIVER, receiver);
  }

  public static String createGetActivitiesRiv21Request(String receiver){
    return String.format(GET_ACTIVITIES_REQUEST_RIV21, receiver);
  }

  public static String createGetActivitiesRiv20Request(String receiver){
    return String.format(GET_ACTIVITIES_REQUEST_RIV20, receiver);
  }

  public static String createGetCertificateRiv20UTF16Request(String receiver){
    return String.format(GET_CERTIFICATE_REQUEST_RIV20_UTF16, receiver);
  }

}
