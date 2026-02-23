package se.skl.tp.vp.integrationtests;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

@CamelSpringBootTest
@SpringBootTest(properties = { "producer.https.hostnameVerification=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@StartTakService
public class FullServiceHostnameVerificationIT extends LeakDetectionBaseTest {

    public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";

    @Autowired
    TestConsumer testConsumer;

    @Autowired
    MockProducer mockHttpsProducer;

    @Value("${vp.instance.id}")
    String vpInstanceId;

    @BeforeEach
    void before() throws Exception {
        mockHttpsProducer.withServerKeystore(null, null).start(HTTPS_PRODUCER_URL);
        TestLogAppender.clearEvents();
    }

    @Test
    void unknownCertificateAtProducerGivesVP009() {
        mockHttpsProducer.setResponseBody("<mocked answer/>");

        Map<String, Object> headers = new HashMap<>();
        headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
        headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
        testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), headers);

        assertEquals(1, TestLogAppender.getNumEvents(MessageLogger.REQ_ERROR));
        String errorLogMsg = TestLogAppender.getEventMessage(MessageLogger.REQ_ERROR,0);
        Assertions.assertNotNull(errorLogMsg);
        assertStringContains(errorLogMsg, "VP009");
        assertStringContains(errorLogMsg, "SSLHandshakeException");
    }
}
