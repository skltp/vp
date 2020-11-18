package se.skl.tp.vp.logging;

import java.net.InetAddress;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.slf4j.helpers.MessageFormatter;
import se.skl.tp.vp.logging.logentry.LogEntry;
import se.skl.tp.vp.logging.logentry.LogMessageExceptionType;
import se.skl.tp.vp.logging.logentry.LogMessageType;
import se.skl.tp.vp.logging.logentry.LogMetadataInfoType;
import se.skl.tp.vp.logging.logentry.LogRuntimeInfoType;

@Log4j2
public class LogMessageFormatter {

  private static final String MSG_ID = "skltp-messages";
  private static final String LOG_STRING = MSG_ID +
      "\n** {}.start ***********************************************************" +
      "\nLogMessage={}\nServiceImpl={}\nHost={} ({})\nComponentId={}\nEndpoint={}\nMessageId={}\nBusinessCorrelationId={}\nExtraInfo={}\nPayload={}" +
      "{}" + // Placeholder for stack trace info if an error is logged
      "\n** {}.end *************************************************************";
  private static final String LOG_STRING_NO_PAYLOAD = MSG_ID +
      "\n** {}.start ***********************************************************" +
      "\nLogMessage={}\nServiceImpl={}\nHost={} ({})\nComponentId={}\nEndpoint={}\nMessageId={}\nBusinessCorrelationId={}\nExtraInfo={}" +
      "{}" + // Placeholder for stack trace info if an error is logged
      "\n** {}.end *************************************************************";

  protected static final String UNKNOWN = "UNKNOWN";
  protected static String hostName = UNKNOWN;
  protected static String hostIp = UNKNOWN;

  static {
    try {
      // Let's give it a try, fail silently...
      InetAddress host = InetAddress.getLocalHost();
      hostName = host.getCanonicalHostName();
      hostIp = host.getHostAddress();
    } catch (Exception ex) {
      log.warn("Failed get runtime values for logging.", ex);
    }
  }

  private LogMessageFormatter() {
    // Static utility class
  }

  protected static String format(String logEventName, LogEntry logEntry) {
    LogMessageType messageInfo  = logEntry.getMessageInfo();
    LogMetadataInfoType metadataInfo = logEntry.getMetadataInfo();
    LogRuntimeInfoType runtimeInfo  = logEntry.getRuntimeInfo();

    String logMessage              = messageInfo.getMessage();
    String serviceImplementation   = metadataInfo.getServiceImplementation();
    String componentId             = runtimeInfo.getComponentId();
    String endpoint                = metadataInfo.getEndpoint();
    String messageId               = runtimeInfo.getMessageId();
    String businessCorrelationId   = runtimeInfo.getBusinessCorrelationId();
    String payload                 = logEntry.getPayload();
    String extraInfoString         = extraInfoToString(logEntry.getExtraInfo());

    StringBuilder stackTrace = new StringBuilder();
    LogMessageExceptionType lmeException = logEntry.getMessageInfo().getException();
    if (lmeException != null) {
      stackTrace.append('\n').append("Stacktrace=").append(lmeException.getStackTrace());
    }
    if (payload == null) {
      return MessageFormatter
          .arrayFormat(LOG_STRING_NO_PAYLOAD, new String[] {logEventName, logMessage, serviceImplementation,
              hostName, hostIp, componentId, endpoint, messageId, businessCorrelationId, extraInfoString, stackTrace.toString(), logEventName}).getMessage();
    } else {
      return MessageFormatter
          .arrayFormat(LOG_STRING, new String[] {logEventName, logMessage, serviceImplementation,
              hostName, hostIp, componentId, endpoint, messageId, businessCorrelationId, extraInfoString, payload, stackTrace.toString(), logEventName}).getMessage();
    }
  }


  private static String extraInfoToString(Map extraInfo) {

    if (extraInfo == null) {
      return "";
    }

    StringBuilder extraInfoString = new StringBuilder();
    extraInfo.forEach((k,v)->extraInfoString.append("\n-").append(k).append("=").append(v));
    return extraInfoString.toString();
  }

}
