package se.skl.tp.vp.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

public class NettyHttpClientExpectContinueTest extends BaseNettyTest {

  @BeforeClass
  public static void startLeakDetection() {
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterClass
  public static void verifyNoLeaks() throws Exception {
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @Override
  protected JndiRegistry createRegistry() throws Exception {
    JndiRegistry registry = super.createRegistry();
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
    Exchange result = template.send("netty4-http:http://localhost:{{port}}/foo?clientInitializerFactory=#continuePipelineFactory", exchange);

    assertFalse(result.isFailed());
    assertEquals("Bye World", result.getIn().getBody(String.class));

    assertMockEndpointsSatisfied();
  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("netty4-http:http://0.0.0.0:{{port}}/foo")
            .to("mock:input")
            .transform().constant("Bye World");
      }
    };
  }

}