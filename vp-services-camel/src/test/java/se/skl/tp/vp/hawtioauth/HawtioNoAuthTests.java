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

@SpringBootTest(classes = {VpServicesApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HawtioNoAuthTests {

  @LocalServerPort
  private int serverPort;

  @Test
  public void disabledLoginTest() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    URI uri = new URI(String.format("http://localhost:%d/actuator/hawtio/", serverPort));

    HttpRequest request = HttpRequest.newBuilder(uri).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Hawtio Management Console"));
  }
}
