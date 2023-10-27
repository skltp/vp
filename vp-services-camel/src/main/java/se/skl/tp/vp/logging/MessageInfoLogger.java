package se.skl.tp.vp.logging;

import org.apache.camel.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import se.skl.tp.vp.logging.logentry.LogEntry;

import java.util.HashMap;


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
    public static final String FAILED_LOG_MESSAGE = "Failed log message: {}";


    public void logReqIn(Exchange exchange) { log(LOGGER_REQ_IN, exchange, MSG_TYPE_LOG_REQ_IN); }

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
            LogEntry logEntry = LogEntryBuilder.createLogEntry(MSG_TYPE_ERROR, exchange, false);
            logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
            logEntry.getMessageInfo().setException(LogEntryBuilder.createMessageException(exchange, stackTrace));
            LOGGER_ERROR.error(LogMessageFormatter.format(LOG_EVENT_ERROR, logEntry));

        } catch (Exception e) {
            LOGGER_ERROR.error(FAILED_LOG_MESSAGE, MSG_TYPE_ERROR, e);
        }
    }

    public void objLogReqIn(Exchange exchange) { objLog(LOGGER_REQ_IN, exchange, MSG_TYPE_LOG_REQ_IN); }
    public void objLogReqOut(Exchange exchange) {
        objLog(LOGGER_REQ_OUT, exchange, MSG_TYPE_LOG_REQ_OUT);
    }

    public void objLogRespIn(Exchange exchange) {
        objLog(LOGGER_RESP_IN, exchange, MSG_TYPE_LOG_RESP_IN);
    }

    public void objLogRespOut(Exchange exchange) {
        objLog(LOGGER_RESP_OUT, exchange, MSG_TYPE_LOG_RESP_OUT);
    }

    public void objLogError(Exchange exchange, String stackTrace) {

        try {
            LogEntry logEntry = LogEntryBuilder.createLogEntry(MSG_TYPE_ERROR, exchange, true);
            logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
            logEntry.getMessageInfo().setException(LogEntryBuilder.createMessageException(exchange, stackTrace));
            HashMap<String, Object> msgMap = new HashMap<>();
            LogMessageFormatter.format(LOG_EVENT_ERROR, logEntry, msgMap);
            LOGGER_ERROR.error(new ObjectMessage(msgMap));

        } catch (Exception e) {
            LOGGER_ERROR.error(FAILED_LOG_MESSAGE, MSG_TYPE_ERROR, e);
        }
    }

    public void log(Logger log, Exchange exchange, String messageType) {
        try {
            if (log.isDebugEnabled()) {
                LogEntry logEntry = LogEntryBuilder.createLogEntry(messageType, exchange, false);
                logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
                logEntry.setPayload(exchange.getIn().getBody(String.class));
                log.debug(LogMessageFormatter.format(LOG_EVENT_DEBUG, logEntry));
            } else if (log.isInfoEnabled()) {
                LogEntry logEntry = LogEntryBuilder.createLogEntry(messageType, exchange, false);
                logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
                log.info(LogMessageFormatter.format(LOG_EVENT_INFO, logEntry));
            }

        } catch (Exception e) {
            log.error(FAILED_LOG_MESSAGE, messageType, e);
        }
    }

    public void objLog(Logger log, Exchange exchange, String messageType) {
        try {
            HashMap<String, Object> msgMap = new HashMap<>();
            LogEntry logEntry = LogEntryBuilder.createLogEntry(messageType, exchange, true);
            logEntry.getExtraInfo().put(LogExtraInfoBuilder.SOURCE, getClass().getName());
            if (log.isDebugEnabled()) {
                logEntry.setPayload(exchange.getIn().getBody(String.class));
                LogMessageFormatter.format(LOG_EVENT_DEBUG, logEntry, msgMap);
                log.debug(new ObjectMessage(msgMap));
            } else if (log.isInfoEnabled()) {
                LogMessageFormatter.format(LOG_EVENT_INFO, logEntry, msgMap);
                log.info(new ObjectMessage(msgMap));
            }

        } catch (Exception e) {
            log.error(FAILED_LOG_MESSAGE, messageType, e);
        }
    }

}
