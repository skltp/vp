package se.skl.tp.vp.camel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.spi.Registry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

public class NettyHttpClientUnexpectedContinueTest extends CamelTestSupport {

  @BeforeAll
  public static void startLeakDetection() {
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterAll
  public static void verifyNoLeaks() throws Exception {
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
      // REALLY important to call super
      super.setUp();
  }
    
  @Test
  public void testHandlingOfUnexpected100Continue() throws Exception {
    getMockEndpoint("mock:input").expectedBodiesReceived("request body");

    HttpUnexpectedContinueServer.startServer(19009);

    String body = "request body";
    DefaultExchange exchange = new DefaultExchange(context);
    exchange.getIn().setBody(body);

    Exchange result = template.send("netty-http:http://localhost:19009", exchange);

    assertFalse(result.isFailed());
    assertTrue(result.getIn().getBody(String.class).startsWith("WELCOME TO THE WILD"));

    HttpUnexpectedContinueServer.stopServer();

  }

}
