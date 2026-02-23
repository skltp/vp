package se.skl.tp.vp.logging;

import org.apache.camel.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.logentry.EcsLogEntry;

import java.util.UUID;

/**
 * ECS (Elastic Common Schema) format message logger implementation.
 */
public class EcsMessageLogger implements MessageLogger {


  private static final Logger LOGGER_REQ_IN = LogManager.getLogger(REQ_IN);
  private static final Logger LOGGER_REQ_OUT = LogManager.getLogger(REQ_OUT);
  private static final Logger LOGGER_RESP_IN = LogManager.getLogger(RESP_IN);
  private static final Logger LOGGER_RESP_OUT = LogManager.getLogger(RESP_OUT);
  private static final Logger LOGGER_ERROR = LogManager.getLogger(REQ_ERROR);

  private static final String MSG_TYPE_LOG_REQ_IN = "req-in";
  private static final String MSG_TYPE_LOG_REQ_OUT = "req-out";
  private static final String MSG_TYPE_LOG_RESP_IN = "resp-in";
  private static final String MSG_TYPE_LOG_RESP_OUT = "resp-out";
  private static final String MSG_TYPE_ERROR = "error";

  /**
   * Default constructor for ECS message logger.
   */
  public EcsMessageLogger() {
    // No configuration needed for ECS logger
  }

  @Override
  public void logReqIn(Exchange exchange) {
    String requestSpanId = UUID.randomUUID().toString();
    exchange.setProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, requestSpanId);
    log(LOGGER_REQ_IN, exchange, MSG_TYPE_LOG_REQ_IN);
  }

  @Override
  public void logReqOut(Exchange exchange) {
    String outboundRequestSpanId = UUID.randomUUID().toString();
    exchange.setProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, outboundRequestSpanId);
    log(LOGGER_REQ_OUT, exchange, MSG_TYPE_LOG_REQ_OUT);
  }

  @Override
  public void logRespIn(Exchange exchange) {
    log(LOGGER_RESP_IN, exchange, MSG_TYPE_LOG_RESP_IN);
    exchange.removeProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN);
  }

  @Override
  public void logRespOut(Exchange exchange) {
    log(LOGGER_RESP_OUT, exchange, MSG_TYPE_LOG_RESP_OUT);
    exchange.removeProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT);
  }

  @Override
  public void logError(Exchange exchange, String stackTrace) {
    try {
      EcsLogEntry ecsLogEntry = new EcsLogEntry.Builder(MSG_TYPE_ERROR)
              .fromExchange(exchange)
              .withHttpForwardHeaders(exchange)
              .withException(exchange, stackTrace)
              .build();
      LOGGER_ERROR.error(ecsLogEntry);

    } catch (Exception e) {
      LOGGER_ERROR.error("Failed log message: {}", MSG_TYPE_ERROR, e);
    }
  }

  void log(Logger log, Exchange exchange, String messageType) {
    try {
      EcsLogEntry ecsLogEntry = getEcsLogEntry(exchange, messageType);
      if (log.isDebugEnabled()) {
        ecsLogEntry = ecsLogEntry.withPayload(exchange.getIn().getBody(String.class));
        log.debug(ecsLogEntry);
      } else if (log.isInfoEnabled()) {
        log.info(ecsLogEntry);
      }
    } catch (Exception e) {
      log.error("Failed log message: {}", messageType, e);
    }
  }

  private @NonNull EcsLogEntry getEcsLogEntry(Exchange exchange, String messageType) {
    return new EcsLogEntry.Builder(messageType)
            .fromExchange(exchange)
            .withHttpForwardHeaders(exchange)
            .build();
  }
}



