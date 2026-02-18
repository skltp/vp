package se.skl.tp.vp.integrationtests.errorhandling;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP008;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_NOT_AUTHORIZED;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.util.HashMap;
import java.util.Map;
import jakarta.xml.soap.SOAPBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.service.TakCacheService;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;
import se.skl.tp.vp.util.soaprequests.SoapUtils;
import se.skltp.takcache.TakCacheLog;
import se.skltp.takcache.TakCacheLog.RefreshStatus;

@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class FullServiceTakErrorIT extends LeakDetectionBaseTest {

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  TakCacheService takCacheService;

  @BeforeEach
  void beforeTest(){
    TestLogAppender.clearEvents();
  }

  @Test
  void shouldGetVP008NoTakServiceAndNoLocalCache() {
    TakCacheLog takCacheLog = takCacheService.refresh();
    assertFalse(takCacheLog.isRefreshSuccessful());
    assertEquals(RefreshStatus.REFRESH_FAILED, takCacheLog.getRefreshStatus());

    Map<String, Object> headers = new HashMap<>();
    String result = testConsumer.sendHttpsRequestToVP(createGetCertificateRequest(RECEIVER_NOT_AUTHORIZED), headers);

    SOAPBody soapBody = SoapUtils.getSoapBody(result);
    assertNotNull(soapBody, "Expected a SOAP message");
    assertTrue(soapBody.hasFault(), "Expected a SOAPFault");

    System.out.printf("Code:%s FaultString:%s%n", soapBody.getFault().getFaultCode(),
        soapBody.getFault().getFaultString());
    assertStringContains(soapBody.getFault().getFaultString(), VP008.getVpDigitErrorCode());

    assertEquals(1,TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
    String errorLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_ERROR,0);
    assertNotNull(errorLogMsg);
    assertStringContains(errorLogMsg, "labels.errorCode=\"VP008\"");
    assertStringContains(errorLogMsg, "error.stack_trace=\"se.skl.tp.vp.exceptions.VpSemanticException: VP008");

  }

}
