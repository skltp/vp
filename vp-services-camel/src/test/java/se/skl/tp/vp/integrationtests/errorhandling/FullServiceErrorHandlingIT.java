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
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NOT_AUTHORIZED;
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
import jakarta.xml.soap.DetailEntry;
import jakarta.xml.soap.Node;
import jakarta.xml.soap.SOAPBody;

import io.undertow.util.FileUtils;
import jakarta.xml.soap.SOAPFault;
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
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.errorhandling.SoapFaultHelper;
import se.skl.tp.vp.exceptions.VPFaultCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;

@CamelSpringBootTest
@SpringBootTest
@TestPropertySource(locations = {"classpath:application.properties","classpath:vp-messages.properties"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
class FullServiceErrorHandlingIT extends LeakDetectionBaseTest {

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockHttpsProducer;

  @Autowired
  private Environment env;

  @Autowired
  ProxyHttpForwardedHeaderProperties proxyHttpForwardedHeaderProperties;

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
  void beforeTest() throws Exception {
    mockHttpsProducer.start(HTTPS_PRODUCER_URL);
    TestLogAppender.clearEvents();
  }

  @Test
  void shouldGetVP002WhenNoCertificateInHTTPCall() {
    Map<String, Object> headers = new HashMap<>();

    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_UNIT_TEST), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP002, new String[]{""}, new String[]{});

    logSoapFault(soapBody);
    assertErrorLog(VP002.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP002");

    String vp002 = env.getProperty("VP002");
    assertRespOutLogWithRespCode500("VP002 [" + instanceName + "] " + vp002);
  }

  @Test
  void shouldGetVP003WhenNoReceiverExists() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(""), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP003, new String[]{""}, new String[]{});

    logSoapFault(soapBody);

    assertErrorLog(VP003.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP003");

    String vp003 = env.getProperty("VP003");
    assertRespOutLogWithRespCode500("VP003 [" + instanceName + "] " + vp003);
  }

  public static final String RECEIVER_HTTPS = "HttpsProducer";

  @Test // If a producer sends soap fault, we shall return to consumer with ResponseCode 500, with the fault embedded in the body.
  void soapFaultPropagatedToConsumerTestIT() {
    mockHttpsProducer.setResponseHttpStatus(500);
    mockHttpsProducer.setResponseBody(SoapFaultHelper.generateSoap11FaultWithCause(REMOTE_SOAP_FAULT,
        VPFaultCodeEnum.Client));
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);

   assertSoapFault(soapBody, VP011,
        new String[]{"Anrop har gjorts utanför TLS vilket ej är tillåtet"},
        new String[]{""});

    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    assertRespOutLogWithRespCode500("VP011");
    assertRespOutLogWithRespCode500("Caller was not on the white list of accepted IP-addresses");
  }

  @Test
  void shouldGetVP004WhenReceiverNotInVagval() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_WITH_NO_VAGVAL), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_WITH_NO_VAGVAL,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY}
    );
    String expectedFaultDetails = String.format("No receiverId (logical address) found for serviceNamespace: %s, receiverId: %s",
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, RECEIVER_WITH_NO_VAGVAL);
    assertExactSoapFaultDetails(soapBody, expectedFaultDetails);
    
    logSoapFault(soapBody);

    assertErrorLog(VP004.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP004");

    String vp004 = env.getProperty("VP004");
    assertRespOutLogWithRespCode500("VP004 [" + instanceName + "] " + vp004);
    assertRespOutLogWithRespCode500(expectedFaultDetails);
  }

  @Test
  void shouldGetVP004WhenReceiverEndsWithWhitespace() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_TRAILING_WHITESPACE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_TRAILING_WHITESPACE,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, "Whitespace detected in incoming request!"}

    );
    logSoapFault(soapBody);
  }

  @Test
  void shouldGetVP004WhenReceiverStartsWithWhitespace() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_LEADING_WHITESPACE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP004,
        new String[]{"Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat"},
        new String[]{"No receiverId (logical address) found for", RECEIVER_LEADING_WHITESPACE,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, "Whitespace detected in incoming request!"}
    );
    logSoapFault(soapBody);
  }

  @Test
  void shouldGetVP005WhenUnknownRivVersionInTAK() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_UNKNOWN_RIVVERSION), headers);
    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP005,
        new String[]{"Tjänsteproducenten stödjer inte anropets angivna rivta-version. Kontrollera uppgifterna."},
        new String[]{"rivtabp20"});

    logSoapFault(soapBody);

    assertErrorLog(VP005.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP005");

    String vp005 = env.getProperty("VP005");
    assertRespOutLogWithRespCode500("VP005 [" + instanceName + "] " + vp005);
    assertRespOutLogWithRespCode500("No receiverId (logical address) with matching Riv-version found for rivtabp20");
  }

  @Test
  void shouldGetVP006WhenMultipleVagvalExist() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_MULTIPLE_VAGVAL), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP006,
        new String[]{"Internt fel i tjänsteplattformen."},
        new String[]{"ReceiverMultipleVagval"});
    logSoapFault(soapBody);

    assertErrorLog(VP006.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP006");

    String vp006 = env.getProperty("VP006");
    assertRespOutLogWithRespCode500("VP006 [" + instanceName + "] " + vp006);
    assertRespOutLogWithRespCode500("More than one receiverId (logical address) with matching Riv-version found for serviceNamespace: " +
            "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1");
  }

  @Test
  void shouldGetVP007WhenReceiverNotAuthorized() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NOT_AUTHORIZED), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP007,
        new String[]{"Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt."},
        new String[]{"Authorization missing for", RECEIVER_NOT_AUTHORIZED,
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY} );
    String expectedFaultDetails = String.format("Authorization missing for serviceNamespace: %s, receiverId: %s, senderId: tp",
            TJANSTEKONTRAKT_GET_CERTIFICATE_KEY, RECEIVER_NOT_AUTHORIZED);
    assertExactSoapFaultDetails(soapBody, expectedFaultDetails);

    logSoapFault(soapBody);

    assertErrorLog(VP007.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP007");

    String vp007 = env.getProperty("VP007");
    assertRespOutLogWithRespCode500("VP007 [" + instanceName + "] " + vp007);
    assertRespOutLogWithRespCode500(expectedFaultDetails);
  }

  @Test
  void shouldGetVP009WhenProducerNotAvailable() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP009,
        new String[]{"Fel vid kontakt med tjänsteproducenten."},
        new String[]{""});

    logSoapFault(soapBody);

    assertErrorLog(VP009.getVpDigitErrorCode(), "error.stack_trace=\"java.net.ConnectException: Cannot connect to");

    String respOutLogMsg = getAndAssertRespOutLog();
    assertStringContains(respOutLogMsg, "event.action=\"resp-out\"");
    assertStringContains(respOutLogMsg, "service.name=\"vp-services-test\"");
    assertStringContains(respOutLogMsg, "VP009");
    assertStringContains(respOutLogMsg, "Error connecting to service producer at address");
    assertExtraInfoLog(respOutLogMsg);
  }

  @Test
  void shouldGetVP010WhenPhysicalAddressEmptyInVagval() {
    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NO_PHYSICAL_ADDRESS), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP010,
        new String[]{"Internt fel i tjänsteplattformen."},
        new String[]{"ReceiverNoPhysicalAddress"});
    assertErrorLog(VP010.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP010");

    String vp010 = env.getProperty("VP010");
    assertRespOutLogWithRespCode500("VP010 [" + instanceName + "] " + vp010);
    assertRespOutLogWithRespCode500("Physical Address field is empty in Service Producer for serviceNamespace: " +
            "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1, receiverId: ReceiverNoPhysicalAddress");
    logSoapFault(soapBody);

  }

  @Test
  void shouldGetVP011ifIpAddressIsNotWhitelisted() {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, "Urken");
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, "dev_env");
    headers.put("X-Forwarded-For", "10.20.30.40");
    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP011,
        new String[]{"Anrop har gjorts utanför TLS vilket ej är tillåtet"},
        new String[]{"10.20.30.40"});
    assertErrorLog(VP011.getVpDigitErrorCode(), "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP011");

    String vp011 = env.getProperty("VP011");
    assertRespOutLogWithRespCode500("VP011 [" + instanceName + "] " + vp011);
    assertRespOutLogWithRespCode500("Anrop har gjorts utanför TLS vilket ej är tillåtet");
    assertRespOutLogWithRespCode500("Caller was not on the white list of accepted IP-addresses.  IP-address: 10.20.30.40. " +
            "HTTP header that caused checking: X-Forwarded-For");
  }

  @Test
  void shouldGetVP013WhenIllegalSender() {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, "SENDER3"); //Not on list sender.id.allowed.list
    headers.put(proxyHttpForwardedHeaderProperties.getAuth_cert(), readPemCertificateFile());
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, TEST_CONSUMER);
    String result = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_NO_PRODUCER_AVAILABLE), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertSoapFault(soapBody, VP013,
        new String[]{"Enligt tjänsteplattformens konfiguration saknar tjänstekonsumenten rätt att använda headern x-rivta-original-serviceconsumer-hsaid."},
        new String[]{"Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid"} );
    assertErrorLog(VP013.getVpDigitErrorCode(), msgVP013);

    String vp013 = env.getProperty("VP013");
    assertRespOutLogWithRespCode500("VP013 [" + instanceName + "] " + vp013);
    assertRespOutLogWithRespCode500("Sender is not approved to set header x-rivta-original-serviceconsumer-hsaid.");
  }

  private void assertSoapFault(SOAPBody soapBody, VpSemanticErrorCodeEnum errorCodeEnum,
      String[] messages, String[] details) {
    assertNotNull(soapBody, "Expected a SOAP message");
    assertTrue(soapBody.hasFault(), "Expected a SOAPFault");
    assertStringContains(soapBody.getFault().getFaultString(), errorCodeEnum.getVpDigitErrorCode());
    assertStringContains(soapBody.getFault().getFaultCode(), errorCodeEnum.getFaultCode());
    for(String message : messages){
      assertStringContains(soapBody.getFault().getFaultString(), message);
    }
    for(String detail : details){
      Iterator<Node> currentDetailChildren = soapBody.getFault().getDetail().getChildElements();
      if(currentDetailChildren.hasNext()){
        DetailEntry detailEntry = (DetailEntry)currentDetailChildren.next();
        String text = detailEntry.getTextContent();
        assertStringContains(text, detail);
      }
    }
  }

  private void assertExactSoapFaultDetails(SOAPBody soapBody, String expectedDetails) {
    assertNotNull(soapBody, "Expected a SOAP message");
    String details = soapBody.getFault().getDetail().getChildElements().next().getValue();
    assertEquals(expectedDetails, details);
  }

  private void assertErrorLog(String code, String message) {
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    String errorLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_ERROR,0);
    assertNotNull(errorLogMsg);
    assertStringContains(errorLogMsg, code);
    assertStringContains(errorLogMsg, message);
  }

  private void assertRespOutLogWithRespCode500(String msg) {
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_OUT));
    String respOutLogMsg = TestLogAppender.getEventMessage(MessageLogger.RESP_OUT,0);
    assertNotNull(respOutLogMsg);
    assertStringContains(respOutLogMsg, msg);
    assertStringContains(respOutLogMsg,"CamelHttpResponseCode\":\"500");
  }

  private String getAndAssertRespOutLog() {
    assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.RESP_OUT));
    return TestLogAppender.getEventMessage(MessageLogger.RESP_OUT,0);
  }

  private void assertExtraInfoLog(String respOutLogMsg) {
    assertStringContains(respOutLogMsg,
        "labels.servicecontract_namespace=\"urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1\"");
    assertStringContains(respOutLogMsg, "labels.senderid=\"tp\"");
    assertStringContains(respOutLogMsg, "labels.receiverid=\"" + se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE);
    assertStringContains(respOutLogMsg, "url.original=\""+ "https://localhost:1974/Im/not/available");
    assertStringContains(respOutLogMsg, "labels.routerVagvalTrace=\"" + se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE);
    assertStringContains(respOutLogMsg, "labels.wsdl_namespace=\"urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20\"");
    assertStringContains(respOutLogMsg, "labels.originalServiceconsumerHsaid=\"tp\"");
    assertStringContains(respOutLogMsg, "labels.rivversion=\"rivtabp20\"");
    assertStringContains(respOutLogMsg, "labels.routerBehorighetTrace=\"" + se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NO_PRODUCER_AVAILABLE);
  }

  private String readPemCertificateFile() {
    URL filePath = FullServiceErrorHandlingIT.class.getClassLoader().getResource("certs/clientPemWithWhiteSpaces.pem");
    assertNotNull(filePath);
    return FileUtils.readFile(filePath);
  }

  private static void logSoapFault(SOAPBody soapBody) {
    assertNotNull(soapBody);
    SOAPFault fault = soapBody.getFault();
    assertNotNull(fault);
    System.out.printf("Code:%s FaultString:%s%n", fault.getFaultCode(), fault.getFaultString());
  }
}
