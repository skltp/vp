package se.skl.tp.vp.httpheader;

import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP013;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;

@Service
@Log4j2
public class OriginalConsumerIdProcessorImpl implements OriginalConsumerIdProcessor {

  @Autowired
  ExceptionUtil exceptionUtil;

  @Value("#{T(java.util.Arrays).asList('${" + PropertyConstants.SENDER_ID_ALLOWED_LIST + ":}')}")
  protected List<String> allowedSenderIds;

  @Value("${" + PropertyConstants.THROW_VP013_WHEN_ORIGNALCONSUMER_NOT_ALLOWED + ":#{false}}")
  protected boolean throwExceptionIfNotAllowed;

  private static final String MSG_SENDER_NOT_ALLOWED_SET_HEADER = "Sender '{}' not allowed to set header {}, accepted senderId's in '{}': [{}]";


  public void process(Exchange exchange) {
    String originalConsumer = exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class);

    if (exchange.getIn().getHeaders().containsKey(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID)) {
      String senderId = exchange.getProperty(VPExchangeProperties.SENDER_ID, String.class);
      if (!senderIsOriginalConsumer(senderId, originalConsumer) && !isSenderAllowedToSetOriginalConsumerIdHeader(senderId)) {
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

  // Special case that allows for AgP calls
  private boolean senderIsOriginalConsumer(String senderId, String originalConsumer) {
    if (senderId == null || originalConsumer == null) return false;
    return senderId.equals(originalConsumer);
  }

}
