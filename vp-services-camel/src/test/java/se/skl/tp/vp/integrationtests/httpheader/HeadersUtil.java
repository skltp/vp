package se.skl.tp.vp.integrationtests.httpheader;

import static org.apache.camel.language.constant.ConstantLanguage.constant;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.undertow.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.constants.HttpHeaders;

public class HeadersUtil {

  public static String TEST_CONSUMER = "aTestConsumer";
  public static String TEST_CORRELATION_ID = "aTestCorrelationId";
  public static String TEST_SENDER = "tp";

  public static Map<String, Object> createHttpsHeaders() {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.SOAP_ACTION, "action");
    return headers;
  }

  public static Map<String, Object> createHttpHeaders() {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, TEST_SENDER);
    // This param is set by config, but is needed by HttpSenderIdExtractorProcessor.java before that.
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, "dev_env");
    // This header is used as alias for the incoming address, when processing access to vp (whitelist)
    headers.put("X-Forwarded-For", constant("1.2.3.4"));
    headers.put(HttpHeaders.SOAP_ACTION, "action");
    return headers;
  }

  public static Map<String, Object> createHttpProxyHeaders(String certAuthHeaderName) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_SENDER_ID, TEST_SENDER);
    // This header is used as alias for the incoming address, when processing access to vp (whitelist)
    headers.put("X-Forwarded-For", constant("1.2.3.4"));
    headers.put(HttpHeaders.SOAP_ACTION, "action");
    URL filePath = HeadersUtil.class.getClassLoader().getResource("certs/clientPemWithWhiteSpaces.pem");
    headers.put(certAuthHeaderName, FileUtils.readFile(filePath));
    return headers;
  }

  public static Map<String, Object> createHttpsHeadersWithCorrId() {
    Map<String, Object> headers = createHttpsHeaders();
    headers.put(HttpHeaders.X_SKLTP_CORRELATION_ID, TEST_CORRELATION_ID);
    return headers;
  }

  public static Map<String, Object> createHttpsHeadersWithOriginalServiceConsumerId() {
    Map<String, Object> headers = createHttpsHeaders();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, TEST_CONSUMER);
    return headers;
  }

  public static Map<String, Object> createHttpHeadersWithMembers() {
    Map<String, Object> headers = createHttpHeaders();
    headers.put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, TEST_CONSUMER);
    headers.put(HttpHeaders.X_SKLTP_CORRELATION_ID, TEST_CORRELATION_ID);
    return headers;
  }

  public static Map<String, Object> createHttpHeadersWithXRivta(Map<String, Object> rivtaHeaders) {
    Map<String, Object> headers = createHttpHeaders();
    headers.putAll(rivtaHeaders);
    return headers;
  }
}
