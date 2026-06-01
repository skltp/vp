package se.skl.tp.vp.integrationtests.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.VpServicesApplication;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

/**
 * Sentinel integration test for management-port separation.
 *
 * <p>When management runs on a dedicated port, actuator health must remain available there and
 * absent from the application port. This guards against regressions in actuator exposure
 * configuration.</p>
 */
@CamelSpringBootTest
@SpringBootTest(
    classes = VpServicesApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.server.port=0"})
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@StartTakService
@SuppressWarnings("SpringBootApplicationProperties")
class ActuatorManagementPortSeparationIT extends LeakDetectionBaseTest {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  @LocalServerPort
  private int serverPort;

  @Value("${local.management.port}")
  private int managementPort;

  /**
   * Verifies actuator health is available on the management port and not mapped on the app port.
   */
  @Test
  void actuatorHealthIsExposedOnManagementPortOnly() throws Exception {
    assertNotEquals(serverPort, managementPort, "This test requires separate app and management ports");

    HttpResponse<String> managementHealth = sendGet("http://localhost:" + managementPort + "/actuator/health");
    assertNotEquals(404, managementHealth.statusCode(), "Actuator health should be mapped on management port");

    HttpResponse<String> appPortHealth = sendGet("http://localhost:" + serverPort + "/actuator/health");
    assertEquals(404, appPortHealth.statusCode(), "Actuator health should not be mapped on app port when management port is separated");
  }

  private HttpResponse<String> sendGet(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .timeout(REQUEST_TIMEOUT)
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

