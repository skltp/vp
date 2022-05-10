package se.skl.tp.vp.httpheader;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.errorhandling.VpCodeMessages;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
@ContextConfiguration(classes = {OriginalConsumerIdProcessorImpl.class, ExceptionUtil.class, VpCodeMessages.class})
@TestPropertySource("classpath:application.properties")
public class OriginalConsumerIdProcessorTest {

  private static final String A_TEST_CONSUMER_ID = "aTestConsumerId";
  private static final String APPROVED_SENDER_ID = "SENDER1";
  private static final String NOT_APPROVED_SENDER_ID = "SENDER3";
  private static final String VP_INSTANCE_ID = "dev_env";

  private boolean originalThrowExceptionIfNotAllowed;
  private List<String> originalAllowedSenderIds;

  private static final String LOG_CLASS = "se.skl.tp.vp.httpheader.OriginalConsumerIdProcessorImpl";

  @Autowired
  OriginalConsumerIdProcessorImpl originalConsumerIdProcessor;

  @BeforeEach
  public void beforeTest() {
    originalThrowExceptionIfNotAllowed = originalConsumerIdProcessor.throwExceptionIfNotAllowed;
    originalAllowedSenderIds = originalConsumerIdProcessor.allowedSenderIds;
    TestLogAppender.clearEvents();

  }

  @AfterEach
  public void after() {
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = originalThrowExceptionIfNotAllowed;
    originalConsumerIdProcessor.allowedSenderIds = originalAllowedSenderIds;
  }

  @Test
  public void senderIsApprovedTest() {
    Exchange exchange = createExchange();

    exchange.setProperty(VPExchangeProperties.SENDER_ID, APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = true;
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));

    assertEquals(0, TestLogAppender.getNumEvents(LOG_CLASS));
  }

  @Test
  public void senderNotApprovedNoVP013Test() {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = false;
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertEquals(1, TestLogAppender.getNumEvents(LOG_CLASS));
    assertEquals(Level.WARN, TestLogAppender.getEvents(LOG_CLASS).get(0).getLevel() );

    final String eventMessage = TestLogAppender.getEventMessage(LOG_CLASS, 0);
    assertTrue(eventMessage.contains(originalConsumerIdProcessor.allowedSenderIds.toString()) );
    assertTrue(eventMessage.contains(NOT_APPROVED_SENDER_ID));
  }

  @Test
  public void senderNotApprovedExpectVP013Test() {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = true;
    try {
      originalConsumerIdProcessor.process(exchange);
      assertTrue(false, "Expected a VP013 exception");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VpSemanticErrorCodeEnum.VP013, vpSemanticException.getErrorCode());
    }
    assertEquals(0, TestLogAppender.getNumEvents(LOG_CLASS));
  }

  @Test
  public void senderNotApprovedButIsNotSettingHeaderShouldBeOKTest() {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = true;
    originalConsumerIdProcessor.process(exchange);
    assertEquals(null, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertEquals(0, TestLogAppender.getNumEvents(LOG_CLASS));

  }

  @Test
  public void senderNotApprovedButCallFromAgpWithHeaderShouldBeOKTest() {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = true;
    originalConsumerIdProcessor.process(exchange);
    assertEquals(NOT_APPROVED_SENDER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertEquals(0, TestLogAppender.getNumEvents(LOG_CLASS));
  }

  @Test
  public void allSendersApprovedTest() {
    Exchange exchange = createExchange();
    exchange.setProperty(VPExchangeProperties.SENDER_ID, NOT_APPROVED_SENDER_ID);
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = true;
    originalConsumerIdProcessor.allowedSenderIds = Collections.emptyList();
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertEquals(0, TestLogAppender.getNumEvents(LOG_CLASS));
  }

  @Test
  public void emptySenderIdNotApprovedTest() {
    Exchange exchange = createExchange();
    exchange.getIn().setHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, A_TEST_CONSUMER_ID);
    originalConsumerIdProcessor.throwExceptionIfNotAllowed = false;
    originalConsumerIdProcessor.process(exchange);
    assertEquals(A_TEST_CONSUMER_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID));
    assertEquals(1, TestLogAppender.getNumEvents(LOG_CLASS));

    assertEquals(Level.WARN, TestLogAppender.getEvents(LOG_CLASS).get(0).getLevel() );
  }


  private Exchange createExchange() {
    CamelContext ctx = new DefaultCamelContext();
    return new DefaultExchange(ctx);
  }

}
