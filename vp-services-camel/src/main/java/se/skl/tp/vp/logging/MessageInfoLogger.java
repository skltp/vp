package se.skl.tp.vp.logging;

import org.apache.camel.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import se.skl.tp.vp.logging.logentry.LogEntry;


public class MessageInfoLogger {

  public static final String REQ_IN = "se.skl.tp.vp.logging.req.in";
  public static final String REQ_OUT = "se.skl.tp.vp.logging.req.out";
  public static final String RESP_IN = "se.skl.tp.vp.logging.resp.in";
  public static final String RESP_OUT = "se.skl.tp.vp.logging.resp.out";
  public static final String REQ_ERROR = "se.skl.tp.vp.logging.error";

  private static final Logger LOGGER_REQ_IN = LogManager.getLogger(REQ_IN);
  private static final Logger LOGGER_REQ_OUT = LogManager.getLogger(REQ_OUT);
  private static final Logger LOGGER_RESP_IN = LogManager.getLogger(RESP_IN);
  private static final Logger LOGGER_RESP_OUT = LogManager.getLogger(RESP_OUT);
  private static final Logger LOGGER_ERROR = LogManager.getLogger(REQ_ERROR);

  private static final String LOG_EVENT_INFO = "logEvent-info";
  private static final String LOG_EVENT_ERROR = "logEvent-error";
  private static final String LOG_EVENT_DEBUG = "logEvent-debug";

  private static final String MSG_TYPE_LOG_REQ_IN = "req-in";
  private static final String MSG_TYPE_LOG_REQ_OUT = "req-out";
  private static final String MSG_TYPE_LOG_RESP_IN = "resp-in";
  private static final String MSG_TYPE_LOG_RESP_OUT = "resp-out";
  private static final String MSG_TYPE_ERROR = "error";

  public void logReqIn(Exchange exchange) {
    log(LOGGER_REQ_IN, exchange, MSG_TYPE_LOG_REQ_IN);
  }

  public void logReqOut(Exchange exchange) {
    log(LOGGER_REQ_OUT, exchange, MSG_TYPE_LOG_REQ_OUT);
  }

  public void logRespIn(Exchange exchange) {
    log(LOGGER_RESP_IN, exchange, MSG_TYPE_LOG_RESP_IN);
  }

  public void logRespOut(Exchange exchange) {
    log(LOGGER_RESP_OUT, exchange, MSG_TYPE_LOG_RESP_OUT);
  }

  public void logError(Exchange exchange, String stackTrace) {

    try {
      LogEntry logEntry = LogEntryBuilder.createLogEntry(MSG_TYPE_ERROR, exchange);
      logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
      logEntry.getMessageInfo().setException(LogEntryBuilder.createMessageException(exchange, stackTrace));
      String logMsg = LogMessageFormatter.format(LOG_EVENT_ERROR, logEntry);
      LOGGER_ERROR.error(logMsg);

    } catch (Exception e) {
      LOGGER_ERROR.error("Failed log message: {}",MSG_TYPE_ERROR, e);
    }
  }

  public void log(Logger log, Exchange exchange, String messageType) {
    try {
      if (log.isDebugEnabled()) {
        LogEntry logEntry = getLogEntry(exchange, messageType);
        logEntry.setPayload(exchange.getIn().getBody(String.class));
        log.debug(LogMessageFormatter.format(LOG_EVENT_DEBUG, logEntry));
      } else if (log.isInfoEnabled()) {
        LogEntry logEntry = getLogEntry(exchange, messageType);
        log.info(LogMessageFormatter.format(LOG_EVENT_INFO, logEntry));
      }
    } catch (Exception e) {
      log.error("Failed log message: {}", messageType, e);
    }
  }

  private @NonNull LogEntry getLogEntry(Exchange exchange, String messageType) {
    LogEntry logEntry = LogEntryBuilder.createLogEntry(messageType, exchange);
    logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
    return logEntry;
  }
}
