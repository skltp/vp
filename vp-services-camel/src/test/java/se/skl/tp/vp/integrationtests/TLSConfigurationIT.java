package se.skl.tp.vp.integrationtests;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.sslcontext.SSLContextParametersConfig;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

/**
 * Integration test that verifies vp.tls.default-config and vp.tls.overrides
 * configuration takes effect when connecting to producers with different TLS requirements.
 */
@SuppressWarnings("JavadocLinkAsPlainText")
@CamelSpringBootTest
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@StartTakService
@TestPropertySource(properties = {
    "spring.ssl.bundle.jks.prod-tls13.keystore.location=classpath:certs/tlsmock/server-keystore.p12",
    "spring.ssl.bundle.jks.prod-tls13.keystore.password=changeit",
    "spring.ssl.bundle.jks.prod-tls13.keystore.type=PKCS12",
    "spring.ssl.bundle.jks.prod-tls13.truststore.location=classpath:certs/tlsmock/client-truststore.jks",
    "spring.ssl.bundle.jks.prod-tls13.truststore.password=changeit",
    "spring.ssl.bundle.jks.prod-tls13.truststore.type=JKS",

    // Configure default TLS config
    "vp.tls.default-config.name=default",
    "vp.tls.default-config.bundle=prod-tls13",

    // Configure override for specific producer (port 19002)
    "vp.tls.overrides[0].name=strict-tls",
    "vp.tls.overrides[0].bundle=prod-tls13",
    "vp.tls.overrides[0].match.domain-suffix=localhost",
    "vp.tls.overrides[0].match.port=19002",
    "vp.tls.overrides[0].protocols-include=TLSv1.3",
    "vp.tls.overrides[0].cipher-suites-include=TLS_AES_128_GCM_SHA256",

    // Configure override for another producer (domain-based)
    "vp.tls.overrides[1].name=legacy-tls",
    "vp.tls.overrides[1].bundle=prod",
    "vp.tls.overrides[1].match.domain-name=localhost",
    "vp.tls.overrides[1].match.port=19003",
    "vp.tls.overrides[1].protocols-include=TLSv1,TLSv1.1,TLSv1.2",
    "vp.tls.overrides[1].cipher-suites-include=TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"
})
public class TLSConfigurationIT extends LeakDetectionBaseTest {

  public static final String HTTPS_PRODUCER_DEFAULT_URL = "https://localhost:19001/vardgivare-b/tjanst2";
  public static final String HTTPS_PRODUCER_STRICT_URL = "https://localhost:19002/vardgivare-c/tjanst3";
  public static final String HTTPS_PRODUCER_LEGACY_URL = "https://localhost:19003/vardgivare-d/tjanst4";

  public static final String RECEIVER_DEFAULT = "HttpsProducer";
  public static final String RECEIVER_STRICT = "HttpsProducerStrict";
  public static final String RECEIVER_LEGACY = "HttpsProducerLegacy";

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  CamelContext camelContext;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  static MockProducer mockProducerDefault;
  static MockProducer mockProducerStrict;
  static MockProducer mockProducerLegacy;

  @BeforeAll
  static void setupMockProducers(@Autowired CamelContext camelContext) throws Exception {
    mockProducerDefault = new MockProducer(camelContext)
            .withServerKeystore("certs/tlsmock/server-keystore.p12", "changeit")
            .withProtocols("TLSv1.3")
            .withCipherSuites("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
    mockProducerStrict = new MockProducer(camelContext)
            .withServerKeystore("certs/tlsmock/server-keystore.p12", "changeit")
            .withProtocols("TLSv1.3")
            .withCipherSuites("TLS_AES_128_GCM_SHA256");
    mockProducerLegacy = new MockProducer(camelContext)
            .withServerKeystore("certs/tp.jks", "password")
            .withProtocols("TLSv1", "TLSv1.1", "TLSv1.2")
            .withCipherSuites("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256");

    mockProducerDefault.start(HTTPS_PRODUCER_DEFAULT_URL);
    mockProducerStrict.start(HTTPS_PRODUCER_STRICT_URL);
    mockProducerLegacy.start(HTTPS_PRODUCER_LEGACY_URL);
  }

  @BeforeEach
  void before() {
    // Reset response bodies before each test
    mockProducerDefault.setResponseBody("response text");
    mockProducerStrict.setResponseBody("response text");
    mockProducerLegacy.setResponseBody("response text");

    TestLogAppender.clearEvents();
  }

  @AfterAll
  static void afterAll() throws Exception {
    // Stop all mock producers
    if (mockProducerDefault != null) {
      mockProducerDefault.stop();
    }
    if (mockProducerStrict != null) {
      mockProducerStrict.stop();
    }
    if (mockProducerLegacy != null) {
      mockProducerLegacy.stop();
    }
  }

  /**
   * Verifies that:
   * - Default SSL context is registered in the Camel registry with ID `SSLContext-default`
   * - Default protocols (TLSv1.3) are configured
   * - Default cipher suites (2 suites) are configured
   * - Requests to producer `HttpsProducer` at `https://localhost:19001` use the default config
   */
  @Test
  void testDefaultTLSConfigIsUsed() {
    String defaultContextId = SSLContextParametersConfig.getId("default");
    SSLContextParameters defaultParams = (SSLContextParameters) camelContext.getRegistry().lookupByName(defaultContextId);
    assertNotNull(defaultParams, "Default SSL context should be registered");
    assertNull(defaultParams.getSecureSocketProtocols());
    assertNull(defaultParams.getCipherSuites());

    mockProducerDefault.setResponseBody("<default-tls-response/>");
    Map<String, Object> headers = createHeaders();
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_DEFAULT), headers);

    assertEquals("<default-tls-response/>", response);
  }

  /**
   * Verifies that:
   * - Strict SSL context is registered with ID `SSLContext-strict-tls`
   * - Only TLSv1.3 protocol is configured
   * - Only one cipher suite (TLS_AES_128_GCM_SHA256) is configured
   * - Requests to producer `HttpsProducerStrict` at `https://localhost:19002` use the strict config
   * - Override matching by port works correctly
   */
  @Test
  void testStrictTLSOverrideIsUsedForPort19002() {
    String strictContextId = SSLContextParametersConfig.getId("strict-tls");
    SSLContextParameters strictParams = (SSLContextParameters) camelContext.getRegistry().lookupByName(strictContextId);
    assertNotNull(strictParams, "Strict SSL context should be registered");

    assertNotNull(strictParams.getSecureSocketProtocols());
    assertEquals(1, strictParams.getSecureSocketProtocols().getSecureSocketProtocol().size());
    assertTrue(strictParams.getSecureSocketProtocols().getSecureSocketProtocol().contains("TLSv1.3"));

    assertNotNull(strictParams.getCipherSuites());
    assertEquals(1, strictParams.getCipherSuites().getCipherSuite().size());
    assertTrue(strictParams.getCipherSuites().getCipherSuite().contains("TLS_AES_128_GCM_SHA256"));

    mockProducerStrict.setResponseBody("<strict-tls-response/>");
    Map<String, Object> headers = createHeaders();
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_STRICT), headers);

    assertEquals("<strict-tls-response/>", response);
  }

  /**
   * Verifies that:
   * - Legacy SSL context is registered with ID `SSLContext-legacy-tls`
   * - Multiple protocols (TLSv1, TLSv1.1, TLSv1.2) are configured
   * - Requests to producer `HttpsProducerLegacy` at `https://localhost:19003` use the legacy config
   * - Override matching by domain name works correctly
   */
  @Test
  void testLegacyTLSOverrideIsUsedForLegacyProducer() {
    String legacyContextId = SSLContextParametersConfig.getId("legacy-tls");
    SSLContextParameters legacyParams = (SSLContextParameters) camelContext.getRegistry().lookupByName(legacyContextId);
    assertNotNull(legacyParams, "Legacy SSL context should be registered");

    assertNotNull(legacyParams.getSecureSocketProtocols());
    assertEquals(3, legacyParams.getSecureSocketProtocols().getSecureSocketProtocol().size());
    assertTrue(legacyParams.getSecureSocketProtocols().getSecureSocketProtocol().contains("TLSv1"));
    assertTrue(legacyParams.getSecureSocketProtocols().getSecureSocketProtocol().contains("TLSv1.1"));
    assertTrue(legacyParams.getSecureSocketProtocols().getSecureSocketProtocol().contains("TLSv1.2"));

    mockProducerLegacy.setResponseBody("<legacy-tls-response/>");
    Map<String, Object> headers = createHeaders();
    String response = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_LEGACY), headers);

    assertEquals("<legacy-tls-response/>", response);
  }

  /**
   * Verifies that:
   * - All three TLS configurations work correctly
   * - Multiple producers with different TLS requirements can be called in sequence
   * - Configurations don't interfere with each other
   */
  @Test
  void testAllThreeProducersCanBeCalledInSequence() {
    // Test that all three configurations work and don't interfere with each other
    mockProducerDefault.setResponseBody("<default/>");
    mockProducerStrict.setResponseBody("<strict/>");
    mockProducerLegacy.setResponseBody("<legacy/>");

    Map<String, Object> headers = createHeaders();

    String response1 = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_DEFAULT), headers);
    assertEquals("<default/>", response1);

    String response2 = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_STRICT), headers);
    assertEquals("<strict/>", response2);

    String response3 = testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_LEGACY), headers);
    assertEquals("<legacy/>", response3);
  }

  private Map<String, Object> createHeaders() {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    return headers;
  }
}
