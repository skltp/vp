package se.skl.tp.vp.integrationtests.getstatus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.status.GetStatusProcessor.KEY_HSA_CACHE_INITIALIZED;
import static se.skl.tp.vp.status.GetStatusProcessor.KEY_NETTY_DIRECT_MEMORY;
import static se.skl.tp.vp.status.GetStatusProcessor.KEY_SERVICE_STATUS;
import static se.skl.tp.vp.status.GetStatusProcessor.KEY_TAK_CACHE_INITIALIZED;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class)
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@StartTakService
public class GetStatusIT extends LeakDetectionBaseTest {

  @Produce
  protected ProducerTemplate producerTemplate;

  @Value("${" + PropertyConstants.VP_HTTP_GET_ROUTE + "}")
  private String getUrl;

  @Test
  public void getStatusResponseTest() {
    String statusResponse = producerTemplate.requestBody("netty-http:" + getUrl, "", String.class);
    assertTrue(statusResponse.startsWith("{") && statusResponse.endsWith("}"));
    assertStringContains(statusResponse, String.format("\"%s\": \"Started\"", KEY_SERVICE_STATUS));
    assertStringContains(statusResponse,
        String.format("\"%s\": \"true\"", KEY_TAK_CACHE_INITIALIZED));
    assertStringContains(statusResponse,
        String.format("\"%s\": \"true\"", KEY_HSA_CACHE_INITIALIZED));
  }

  @Test
  public void getStatusResponseWithMemoryTest() {
    String statusResponse = producerTemplate
        .requestBody(String.format("netty-http:%s?memory", getUrl), "", String.class);
    assertTrue(statusResponse.startsWith("{") && statusResponse.endsWith("}"));
    assertStringContains(statusResponse, String.format("\"%s\": \"Started\"", KEY_SERVICE_STATUS));
    assertStringContains(statusResponse,
        String.format("\"%s\": \"true\"", KEY_TAK_CACHE_INITIALIZED));
    assertStringContains(statusResponse,
        String.format("\"%s\": \"true\"", KEY_HSA_CACHE_INITIALIZED));
    assertStringContains(statusResponse,
        String.format("\"%s\": \"Direct:", KEY_NETTY_DIRECT_MEMORY));
  }

  @Test
  public void getStatusResponseWithNettyTest() {
    String statusResponse = producerTemplate
        .requestBody(String.format("netty-http:%s?netty", getUrl), "", String.class);
    assertTrue(statusResponse.startsWith("{") && statusResponse.endsWith("}"));
    assertStringContains(statusResponse, "DirectArena1");
    assertStringContains(statusResponse, "NettyTotal");
  }
}
