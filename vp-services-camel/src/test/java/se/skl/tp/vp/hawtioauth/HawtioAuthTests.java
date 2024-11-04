package se.skl.tp.vp.hawtioauth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.VpServicesApplication;

@SpringBootTest(classes = {VpServicesApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "hawtio.authentication.enabled=true", "hawtio.external.loginfile=src/test/resources/hawtio_users.properties" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HawtioAuthTests {

  @LocalServerPort
  private int serverPort;

  @Test
  public void doLoginTest() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    URI uri = new URI(String.format("http://localhost:%d/actuator/hawtio/auth/login", serverPort));
    String body = "{ \"username\": \"testuser\", \"password\": \"test\" }";

    HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("set-cookie").isPresent());
  }

  @Test
  public void failedLoginTest() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    URI uri = new URI(String.format("http://localhost:%d/actuator/hawtio/auth/login", serverPort));
    String body = "{ \"username\": \"wronguser\", \"password\": \"anything\" }";

    HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(403, response.statusCode());
    assertFalse(response.headers().firstValue("set-cookie").isPresent());
  }
}
