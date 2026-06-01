package se.skl.tp.vp.integrationtests.actuator;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.VpServicesApplication;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

/**
 * Sentinel integration test that verifies actuator health endpoints are mapped.
 *
 * <p>The assertions intentionally check for "not 404" instead of pinning exact HTTP status codes.
 * This keeps the test stable across Spring Boot and actuator behavior changes while still catching
 * regressions where actuator endpoints are no longer exposed.</p>
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
class ActuatorAvailabilityIT extends LeakDetectionBaseTest {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  @LocalServerPort
  private int serverPort;

  @Value("${local.management.port}")
  private int managementPort;

  @Autowired
  private CamelContext camelContext;

  /**
   * Verifies the base health endpoint is mapped on the management port.
   */
  @Test
  void healthEndpointIsMappedOnManagementPort() throws Exception {
    assertTrue(camelContext.getStatus().isStarted(), "Camel context should be started");
    assertNotEquals(serverPort, managementPort, "Management and app port should differ in this test");

    HttpResponse<String> response = awaitEndpointMapping(managementUrl("/actuator/health"));
    assertNotEquals(404, response.statusCode(), "Health endpoint should be mapped on management port");
  }

  /**
   * Verifies the liveness endpoint is mapped on the management port.
   */
  @Test
  void livenessEndpointIsMappedOnManagementPort() throws Exception {
    HttpResponse<String> response = awaitEndpointMapping(managementUrl("/actuator/health/liveness"));
    assertNotEquals(404, response.statusCode(), "Liveness endpoint should be mapped on management port");
  }

  /**
   * Verifies the readiness endpoint is mapped on the management port.
   */
  @Test
  void readinessEndpointIsMappedOnManagementPort() throws Exception {
    HttpResponse<String> response = awaitEndpointMapping(managementUrl("/actuator/health/readiness"));
    assertNotEquals(404, response.statusCode(), "Readiness endpoint should be mapped on management port");
  }

  private String managementUrl(String path) {
    return "http://localhost:" + managementPort + path;
  }

  /**
   * Polls until an endpoint is mapped (status code other than 404), or times out.
   */
  private HttpResponse<String> awaitEndpointMapping(String url) throws Exception {
    long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
    HttpResponse<String> lastResponse = null;
    Exception lastException = null;

    while (System.nanoTime() < deadline) {
      try {
        HttpResponse<String> response = sendGet(url);
        lastResponse = response;
        if (response.statusCode() != 404) {
          return response;
        }
      } catch (Exception e) {
        lastException = e;
      }
      Thread.sleep(500);
    }

    if (lastResponse != null) {
      throw new AssertionError("Endpoint not mapped for " + url + ": " + lastResponse.statusCode()
          + ", body: " + lastResponse.body());
    }

    throw new AssertionError("Could not reach " + url + " within timeout", lastException);
  }

  private HttpResponse<String> sendGet(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .timeout(REQUEST_TIMEOUT)
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

