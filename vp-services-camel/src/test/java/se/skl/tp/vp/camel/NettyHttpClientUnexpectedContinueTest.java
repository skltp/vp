package se.skl.tp.vp.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

public class NettyHttpClientUnexpectedContinueTest extends CamelTestSupport {

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
  public void testHandlingOfUnexpected100Continue() throws Exception {
    getMockEndpoint("mock:input").expectedBodiesReceived("request body");

    HttpUnexpectedContinueServer.startServer(19009);

    String body = "request body";
    DefaultExchange exchange = new DefaultExchange(context);
    exchange.getIn().setBody(body);

    Exchange result = template.send("netty4-http:http://localhost:19009?clientInitializerFactory=#continuePipelineFactory", exchange);

    assertFalse(result.isFailed());
    assertTrue(result.getIn().getBody(String.class).startsWith("WELCOME TO THE WILD"));

    HttpUnexpectedContinueServer.stopServer();

  }


}
