package se.skl.tp.vp.camel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

public class NettyHttpClientExpectContinueTest extends BaseNettyTest {

  @BeforeAll
  public static void startLeakDetection() {
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterAll
  public static void verifyNoLeaks() throws Exception {
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @Override
  protected Registry createCamelRegistry() throws Exception {
    Registry registry = super.createCamelRegistry();

    Properties prop = new Properties();
    prop.setProperty("port", "" + getPort());
    registry.bind("continuePipelineFactory", new VPHttpClientPipelineFactory());
    return registry;
  }

  @Test
  public void testHttpExpect100Continue() throws Exception {
    getMockEndpoint("mock:input").expectedBodiesReceived("request body");

    String body = "request body";
    DefaultExchange exchange = new DefaultExchange(context);

    exchange.getIn().setHeader("Expect", "100-continue");
    exchange.getIn().setBody(body);

    System.out.println("Port:"+ getPort());
    Exchange result = template.send("netty-http:http://localhost:{{port}}/foo?clientInitializerFactory=#continuePipelineFactory", exchange);

    assertFalse(result.isFailed());
    assertEquals("Bye World", result.getIn().getBody(String.class));

    assertMockEndpointsSatisfied();
  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("netty-http:http://0.0.0.0:{{port}}/foo")
            .to("mock:input")
            .transform().constant("Bye World");
      }
    };
  }

}