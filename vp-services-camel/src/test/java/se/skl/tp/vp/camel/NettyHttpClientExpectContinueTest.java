package se.skl.tp.vp.camel;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
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
  
  @Test
  public void testHttpExpect100Continue() throws Exception {

	getMockEndpoint("mock:input").expectedBodiesReceived("request body");

    String body = "request body";
    DefaultExchange exchange = new DefaultExchange(context);

    exchange.getIn().setHeader("Expect", "100-continue");
    exchange.getIn().setBody(body);

    System.out.println("Port:"+ getPort());
    String url = String.format("netty-http:http://localhost:%s/foo", getPort());
    Exchange result = template.send(url, exchange);
    assertFalse(result.isFailed());
    assertEquals("Bye World", result.getIn().getBody(String.class));

    assertIsSatisfied(context);
  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
    	  String url = String.format("netty-http:http://0.0.0.0:%s/foo", getPort());
        from(url)
            .to("mock:input")
            .transform().constant("Bye World");
      }
    };
  }

}