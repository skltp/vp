package se.skl.tp.vp.httpheader;

import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP013;

import java.util.Arrays;
import java.util.Set;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;

@Service
@Log4j2
public class OriginalConsumerIdProcessorImpl implements OriginalConsumerIdProcessor {

  private final ExceptionUtil exceptionUtil;

  protected Set<String> allowedSenderIds;

  @Value("${" + PropertyConstants.THROW_VP013_WHEN_ORIGNALCONSUMER_NOT_ALLOWED + ":#{false}}")
  protected boolean throwExceptionIfNotAllowed;

  @Value("${" + PropertyConstants.VP_INSTANCE_ID + ":#{null}}")
  protected String vpInstanceId;

  private static final String MSG_SENDER_NOT_ALLOWED_SET_HEADER = "Sender '{}' not allowed to set header {}, accepted senderId's in '{}': [{}]";

  public OriginalConsumerIdProcessorImpl(@Value("${" + PropertyConstants.SENDER_ID_ALLOWED_LIST + "}") String senderIdAllowedList, ExceptionUtil exceptionUtil) {
    this.exceptionUtil = exceptionUtil;
    setAllowedSenderIds(senderIdAllowedList);
  }

  private void setAllowedSenderIds(String senderIdAllowedList) {
    if (senderIdAllowedList == null || senderIdAllowedList.isBlank()) {
      this.allowedSenderIds = Set.of();
    } else {
      this.allowedSenderIds = Arrays.stream(senderIdAllowedList.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(java.util.stream.Collectors.toSet());
    }
  }

  public void process(Exchange exchange) {
    String originalConsumer = exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class);

    if (exchange.getIn().getHeaders().containsKey(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID)) {
      String senderId = exchange.getProperty(VPExchangeProperties.SENDER_ID, String.class);
      if (!isInternalCall(exchange) && !isSenderAllowedToSetOriginalConsumerIdHeader(senderId)) {
        if (throwExceptionIfNotAllowed) {
          throw exceptionUtil.createVpSemanticException(VP013);
        }
        log.warn(MSG_SENDER_NOT_ALLOWED_SET_HEADER, senderId, HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, PropertyConstants.SENDER_ID_ALLOWED_LIST, allowedSenderIds.toString());
      }
    }

    exchange.setProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID, originalConsumer);
    
    // This property should always be populated.
    exchange.setProperty(VPExchangeProperties.OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID, originalConsumer);
  }

  public boolean isSenderAllowedToSetOriginalConsumerIdHeader(String senderId) {
    return allowedSenderIds.isEmpty() || (senderId != null && allowedSenderIds.contains(senderId.trim()));
  }

  private boolean isInternalCall(Exchange exchange) {
    if (vpInstanceId == null) return false;
    String senderVpInstanceId = exchange.getIn().getHeader(HttpHeaders.X_VP_INSTANCE_ID, String.class);
    return vpInstanceId.equals(senderVpInstanceId);
  }

}
