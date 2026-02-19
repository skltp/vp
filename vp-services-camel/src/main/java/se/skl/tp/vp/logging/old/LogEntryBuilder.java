package se.skl.tp.vp.logging.old;

import java.util.Map;
import org.apache.camel.Exchange;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.logging.old.logentry.LogEntry;
import se.skl.tp.vp.logging.old.logentry.LogMessageExceptionType;
import se.skl.tp.vp.logging.old.logentry.LogMessageType;
import se.skl.tp.vp.logging.old.logentry.LogMetadataInfoType;
import se.skl.tp.vp.logging.old.logentry.LogRuntimeInfoType;

public class LogEntryBuilder {

  private LogEntryBuilder() {
    // Static utility class
  }

  public static LogEntry createLogEntry(
      String logMessageType,
      Exchange exchange) {

    LogRuntimeInfoType lri = createRunTimeInfo(exchange);
    LogMetadataInfoType lmi = createMetadataInfo(exchange);
    LogMessageType lm = createLogMessage(logMessageType);

    // Create the log entry object
    LogEntry logEntry = new LogEntry();
    logEntry.setMetadataInfo(lmi);
    logEntry.setRuntimeInfo(lri);
    logEntry.setMessageInfo(lm);

    Map<String, String> extraInfo = LogExtraInfoBuilder.createExtraInfo(exchange);
    logEntry.setExtraInfo(extraInfo);

    return logEntry;
  }

  public static LogMessageExceptionType createMessageException(Exchange exchange, String stackTrace) {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    if (throwable == null) {
      return null;
    }

    LogMessageExceptionType lme = new LogMessageExceptionType();
    lme.setExceptionClass(throwable.getClass().getName());
    lme.setExceptionMessage(throwable.getMessage());
    lme.setStackTrace(stackTrace);

    return lme;
  }

  private static LogMessageType createLogMessage(String logMessageType) {
    LogMessageType lm = new LogMessageType();
    lm.setMessage(logMessageType);
    return lm;
  }

  private static LogMetadataInfoType createMetadataInfo(Exchange exchange) {

    String serviceImplementation = "";
    String endpoint = "";

    if (exchange != null) {
      serviceImplementation = exchange.getFromRouteId();
      String endpointURI = exchange.getProperty(VPExchangeProperties.HTTP_URL_IN, String.class);
      endpoint = (endpointURI == null) ? "" : endpointURI;

    }

    LogMetadataInfoType lmi = new LogMetadataInfoType();
    lmi.setServiceImplementation(serviceImplementation);
    lmi.setEndpoint(endpoint);

    return lmi;
  }

  private static LogRuntimeInfoType createRunTimeInfo(Exchange exchange) {
    String messageId = "";
    String businessCorrelationId = "";
    String componentId = "";

    if (exchange != null) {
      businessCorrelationId = exchange.getProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "", String.class);
      messageId = exchange.getMessage().getMessageId();

      // The context name is set in property "camel.springboot.name"
      componentId = exchange.getContext().getName();
    }

    LogRuntimeInfoType lri = new LogRuntimeInfoType();
    lri.setComponentId(componentId);
    lri.setMessageId(messageId);
    lri.setBusinessCorrelationId(businessCorrelationId);
    return lri;
  }


}
