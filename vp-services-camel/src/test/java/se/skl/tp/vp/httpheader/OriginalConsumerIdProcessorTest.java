package se.skl.tp.vp.httpheader;


import static org.junit.Assert.assertEquals;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.errorhandling.VpCodeMessages;

@RunWith( SpringRunner.class )
@ContextConfiguration(classes = {OriginalConsumerIdProcessorImpl.class, ExceptionUtil.class, VpCodeMessages.class, CheckSenderAllowedToUseHeaderImpl.class})
@TestPropertySource("classpath:application.properties")
public class OriginalConsumerIdProcessorTest {

  private static final String A_TEST_CONSUMER_ID = "aTestConsumerId";
  private static final String APPROVED_SENDER_ID = "SENDER1";
  private static final String NOT_APPROVED_SENDER_ID = "SENDER3";

  private boolean configuredValue;

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Autowired
  OriginalConsumerIdProcessorImpl originalConsumerIdProcessor;

  @Before
  public void beforeTest(){
    configuredValue = originalConsumerIdProcessor.isEnforceSenderIdCheck();
  }

  @After
  public void after(){
    originalConsumerIdProcessor.setEnforceSenderIdCheck(configuredValue);
  }

  @Test
  public void senderTestedForUseOfXrivtaOriginalConsumerId() throws Exception {
    Exchange exchange = createExchange();

    exchange.setProperty(VPExchangeProperties.SENDER_ID, APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.setEnforceSenderIdCheck(true);
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
  }

  @Test
  public void senderNotTestedForUseOfXrivtaOriginalConsumerId() throws Exception {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.setEnforceSenderIdCheck(false);
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
  }


  private Exchange createExchange() {
    CamelContext ctx = new DefaultCamelContext();
    return new DefaultExchange(ctx);
  }
}
