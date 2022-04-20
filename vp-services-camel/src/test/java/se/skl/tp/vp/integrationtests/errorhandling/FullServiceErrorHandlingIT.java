package se.skl.tp.vp.integrationtests.errorhandling;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP002;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP003;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP004;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP005;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP006;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP007;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP009;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP010;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP011;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP013;
import static se.skl.tp.vp.integrationtests.httpheader.HeadersUtil.TEST_CONSUMER;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_LEADING_WHITESPACE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_MULTIPLE_VAGVAL;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NOT_AUHORIZED;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PHYSICAL_ADDRESS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_TRAILING_WHITESPACE;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNKNOWN_RIVVERSION;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_WITH_NO_VAGVAL;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.TJANSTEKONTRAKT_GET_CERTIFICATE_KEY;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.SOAPBody;

import io.undertow.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.errorhandling.SoapFaultHelper;
import se.skl.tp.vp.exceptions.VPFaultCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

@CamelSpringBootTest
@SpringBootTest
@TestPropertySource(locations = {"classpath:application.properties","classpath:vp-messages.properties"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
public class FullServiceErrorHandlingIT extends LeakDetectionBaseTest {

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockHttpsProducer;

  @Autowired
  private Environment env;

  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";


  public static final String REMOTE_SOAP_FAULT =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
          "  <soapenv:Header/>  <soapenv:Body>    <soapenv:Fault>" +
          "      <faultcode>soap:Client</faultcode>" +
          "      <faultstring>VP011 [NTjP Remote] Anrop har gjorts utanför TLS vilket ej är tillåtet. Tjänstekonsumenten ska alltid använda TLS för säker kommunikation.</faultstring>" +
          "      <details>Caller was not on the white list of accepted IP-addresses. IP-address: 84.17.194.105. HTTP header that caused checking: x-vp-sender-id (se.skl.tp.vp.exceptions.VpSemanticException). Message payload is of type: ReversibleXMLStreamReader</details>" +
          "    </soapenv:Fault>  </soapenv:Body></soapenv:Envelope>";

  @Value("VP013")
  String msgVP013;

  @Value("${" + PropertyConstants.VP_INSTANCE_NAME + "}")
  String instanceName;

  @BeforeEach
  public void beforeTest(){
    try {
      mockHttpsProducer.start(HTTPS_PRODUCER_URL + "?sslContextParameters=#outgoingSSLContextParameters&ssl=true");
    } catch (Exception e) {
      e.printStackTrace();
    }
    TestLogAppender.clearEvents();
  }

  @Test
  public void shouldGetVP002WhenNoCertificateInHTTPCall() throws Exception {
    Map<String, Object> headers = new HashMap<>();

    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_UNIT_TEST), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP002, new String[]{""}, new String[]{});

    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP002.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP002");

    String VP002_error= env.getProperty("VP002");
    assertRespOutLog("VP002 [" + instanceName + "] " + VP002_error);
  }

  @Test
  public void shouldGetVP003WhenNoReceieverExist() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(""), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP003, new String[]{""}, new String[]{});

    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP003.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP003");

    String VP003_error = env.getProperty("VP003");
    assertRespOutLog("VP003 [" + instanceName + "] " + VP003_error);
  }

  public static final String RECEIVER_HTTPS = "HttpsProducer";

  @Test // If a producer sends soap fault, we shall return to consumer with ResponseCode 500, with the fault embedded in the body.
  public void soapFaultPropagatedToConsumerTestIT() throws Exception {
    mockHttpsProducer.setResponseHttpStatus(500);
    mockHttpsProducer.setResponseBody(SoapFaultHelper.generateSoap11FaultWithCause(REMOTE_SOAP_FAULT,
        VPFaultCodeEnum.Client));
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);

   assertSoapFault(soapBody, VP011,
        new String[]{"Anrop har gjorts utanför TLS vilket ej är tillåtet"},
        new String[]{""});

    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    assertRespOutLogWithRespCode500("VP011");
    assertRespOutLogWithRespCode500("Caller was not on the white list of accepted IP-addresses");
  }

  @Test
  public void shouldGetVP004WhenRecieverNotInVagval() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_WITH_NO_VAGVAL), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_WITH_NO_VAGVAL,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY}

    );
    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP004.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP004");

    String VP004_error = env.getProperty("VP004");
    assertRespOutLog("VP004 [" + instanceName + "] " + VP004_error);
    assertRespOutLog("No receiverId (logical address) found for serviceNamespace: " +
            "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1, receiverId: NoVagvalReceiver");
  }

  @Test
  public void shouldGetVP004WhenRecieverEndsWithWhitespace() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_TRAILING_WHITESPACE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_TRAILING_WHITESPACE,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, "Whitespace detected in incoming request!"}

    );
    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());
  }

  @Test
  public void shouldGetVP004WhenRecieverStartsWithWhitespace() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_LEADING_WHITESPACE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_LEADING_WHITESPACE,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, "Whitespace detected in incoming request!"}
    );
    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());
  }

  @Test
  public void shouldGetVP005WhenUnkownRivVersionInTAK() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_UNKNOWN_RIVVERSION), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP005,
        new String[]{"Tjänsteproducenten stödjer inte anropets angivna rivta-version. Kontrollera uppgifterna."},
        new String[]{"rivtabp20"});

    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP005.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP005");

    String VP005error = env.getProperty("VP005");
    assertRespOutLog("VP005 [" + instanceName + "] " + VP005error);
    assertRespOutLog("No receiverId (logical address) with matching Riv-version found for rivtabp20");
  }

  @Test
  public void shouldGetVP006WhenMultipleVagvalExist() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_MULTIPLE_VAGVAL), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP006,
        new String[]{"Internt fel i tjänsteplattformen."},
        new String[]{"RecevierMultipleVagval"});
    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP006.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP006");

    String VP006error = env.getProperty("VP006");
    assertRespOutLog("VP006 [" + instanceName + "] " + VP006error);
    assertRespOutLog("More than one receiverId (logical address) with matching Riv-version found for serviceNamespace: " +
            "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");
  }

  @Test
  public void shouldGetVP007WhenRecieverNotAuhtorized() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NOT_AUHORIZED), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP007,
        new String[]{"Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt."},
        new String[]{"Authorization missing for", RECEIVER_NOT_AUHORIZED,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY} );

    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP007.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP007");

    String VP007error = env.getProperty("VP007");
    assertRespOutLog("VP007 [" + instanceName + "] " + VP007error);
    assertRespOutLog("Authorization missing for serviceNamespace: urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");
  }

  @Test
  public void shouldGetVP009WhenProducerNotAvailable() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP009,
        new String[]{"Fel vid kontakt med tjänsteproducenten."},
        new String[]{""});

    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

    assertErrorLog(VP009.getVpDigitErrorCode(), "Stacktrace=java.net.ConnectException: Cannot connect to");

    String respOutLogMsg = getAndAssertRespOutLog();
    assertStringContains(respOutLogMsg, "LogMessage=resp-out");
    assertStringContains(respOutLogMsg, "ComponentId=vp-services");
    assertStringContains(respOutLogMsg, "VP009");
    assertStringContains(respOutLogMsg, "Error connecting to service producer at address");
    assertExtraInfoLog(respOutLogMsg, RECEIVER_NO_PRODUCER_AVAILABLE, "https://localhost:1974/Im/not/available");
  }

  @Test
  public void shouldGetVP010WhenPhysicalAdressEmptyInVagval() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PHYSICAL_ADDRESS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP010,
        new String[]{"Internt fel i tjänsteplattformen."},
        new String[]{"RecevierNoPhysicalAddress"});
    assertErrorLog(VP010.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP010");

    String VP010error = env.getProperty("VP010");
    assertRespOutLog("VP010 [" + instanceName + "] " + VP010error);
    assertRespOutLog("Physical Address field is empty in Service Producer for serviceNamespace: " +
            "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1, receiverId: RecevierNoPhysicalAddress");
    System.out.printf("Code:%s FaultString:%s\n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());

  }

  @Test
  public void shouldGetVP011ifIpAddressIsNotWhitelisted() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, "Urken");
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, "dev_env");
    headers.put("X-Forwarded-For", "10.20.30.40");
    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP011,
        new String[]{"Anrop har gjorts utanför TLS vilket ej är tillåtet"},
        new String[]{"10.20.30.40"});
    assertErrorLog(VP011.getVpDigitErrorCode(), "Stacktrace=se.skl.tp.vp.exceptions.VpSemanticException: VP011");

    String VP011error = env.getProperty("VP011");
    assertRespOutLog("VP011 [" + instanceName + "] " + VP011error);
    assertRespOutLog("Anrop har gjorts utanför TLS vilket ej är tillåtet");
    assertRespOutLog("Caller was not on the white list of accepted IP-addresses.  IP-address: 10.20.30.40. " +
            "HTTP header that caused checking: X-Forwarded-For");
  }

  @Test
  public void shouldGetVP013WhenIllegalSender() throws Exception {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, "SENDER3"); //Not on list sender.id.allowed.list
    headers.put(HttpHeaders.CERTIFICATE_FROM_REVERSE_PROXY, readPemCertificateFile("certs/clientPemWithWhiteSpaces.pem"));
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, TEST_CONSUMER);
    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP013,
        new String[]{"Enligt tjänsteplattformens konfiguration saknar tjänstekonsumenten rätt att använda headern x-rivta-original-serviceconsumer-hsaid."},
        new String[]{"Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid"} );
    assertErrorLog(VP013.getVpDigitErrorCode(), msgVP013);

    String VP013error = env.getProperty("VP013");
    assertRespOutLog("VP013 [" + instanceName + "] " + VP013error);
    assertRespOutLog("Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid.");
  }

  private void assertSoapFault(SOAPBody soapBody, VpSemanticErrorCodeEnum errorCodeEnum,
      String[] messages, String[] details) {
    assertNotNull(soapBody, "Expected a SOAP message");
    assertNotNull(soapBody.hasFault(), "Expected a SOAPFault");
    assertStringContains(soapBody.getFault().getFaultString(), errorCodeEnum.getVpDigitErrorCode());
    assertStringContains(soapBody.getFault().getFaultCode(), errorCodeEnum.getFaultCode());
    for(String message : messages){
      assertStringContains(soapBody.getFault().getFaultString(), message);
    }
    for(String detail : details){
      Iterator currentDetailChildren = soapBody.getFault().getDetail().getChildElements();
      if(currentDetailChildren.hasNext()){
        DetailEntry detailEntry = (DetailEntry)currentDetailChildren.next();
        String text = detailEntry.getTextContent();
        assertStringContains(text, detail);
      }
    }
  }

  private void assertErrorLog(String code, String message) {
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.REQ_ERROR));
    String errorLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.REQ_ERROR,0);
    assertStringContains(errorLogMsg, code);
    assertStringContains(errorLogMsg, message);
  }

  private void assertRespOutLog(String msg) {
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
    assertStringContains(respOutLogMsg, msg);
    assertStringContains(respOutLogMsg,"CamelHttpResponseCode=500");
  }

  private void assertRespOutLogWithRespCode200(String msg) {
	    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
	    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
	    assertStringContains(respOutLogMsg, msg);
	    assertStringContains(respOutLogMsg,"CamelHttpResponseCode=200");
	  }

  private void assertRespOutLogWithRespCode500(String msg) {
	    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
	    String respOutLogMsg = TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
	    assertStringContains(respOutLogMsg, msg);
	    assertStringContains(respOutLogMsg,"CamelHttpResponseCode=500");
	  }

  private String getAndAssertRespOutLog() {
    assertEquals(1, TestLogAppender.getNumEvents(MessageInfoLogger.RESP_OUT));
    return TestLogAppender.getEventMessage(MessageInfoLogger.RESP_OUT,0);
  }

  private void assertExtraInfoLog(String respOutLogMsg, String expectedReceiverId, String expectedProducerUrl) {
    assertStringContains(respOutLogMsg, "-senderIpAdress=");
    assertStringContains(respOutLogMsg,
        "-servicecontract_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");
    assertStringContains(respOutLogMsg, "-senderid=tp");
    assertStringContains(respOutLogMsg, "-receiverid=" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "-endpoint_url="+expectedProducerUrl);
    assertStringContains(respOutLogMsg, "-routerVagvalTrace=" + expectedReceiverId);
    assertStringContains(respOutLogMsg, "-wsdl_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20");
    assertStringContains(respOutLogMsg, "-originalServiceconsumerHsaid=tp");
    assertStringContains(respOutLogMsg, "-rivversion=rivtabp20");
    assertStringContains(respOutLogMsg, "-routerBehorighetTrace=" + expectedReceiverId);
  }

  private String readPemCertificateFile(String pemFile) {
    URL filePath = FullServiceErrorHandlingIT.class.getClassLoader().getResource(pemFile);
    String pemCertContent = FileUtils.readFile(filePath);
    return pemCertContent;
  }
}
