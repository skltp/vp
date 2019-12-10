package se.skl.tp.vp.constants;

public class VPExchangeProperties {

  private VPExchangeProperties() {
    //To hide implicit public constructor. Sonar suggestion.
  }

  public static final String VAGVAL = "vagval";
  public static final String VAGVAL_HOST = "vagvalHost";
  public static final String SENDER_ID = "senderid";
  public static final String SENDER_IP_ADRESS = "senderIpAdress";
  public static final String OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaidOut";
  public static final String IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaidIn";
  public static final String SKLTP_CORRELATION_ID = "skltp_correlationId";
  public static final String RECEIVER_ID = "receiverid";
  public static final String RIV_VERSION = "rivversion";
  public static final String RIV_VERSION_OUT = "rivversion_out";
  public static final String SERVICECONTRACT_NAMESPACE = "servicecontract_namespace";
  public static final String XML_REQUEST_ENCODING = "XmlRequestEncoding";
  public static final String ORIGINAL_REQUEST_ENCODING = "OriginalRequestEncoding";

  public static final String SESSION_ERROR = "sessionStatus";
  public static final String SESSION_ERROR_DESCRIPTION = "sessionErrorDescription";
  public static final String SESSION_ERROR_TECHNICAL_DESCRIPTION = "sessionErrorTechnicalDescription";
  public static final String SESSION_ERROR_CODE = "errorCode";
  public static final String SESSION_HTML_STATUS = "statusCode";

  public static final String VAGVAL_TRACE = "routerVagvalTrace";
  public static final String ANROPSBEHORIGHET_TRACE = "routerBehorighetTrace";

  public static final String HTTP_URL_IN = "HttpUrlIn";
  public static final String HTTP_URL_OUT = "HttpUrlOut";

  public static final String VP_X_FORWARDED_PROTO = "httpXForwardedProto";
  public static final String VP_X_FORWARDED_HOST = "httpXForwardedHost";
  public static final String VP_X_FORWARDED_PORT = "httpXForwardedPort";


}
