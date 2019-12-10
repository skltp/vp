package se.skl.tp.vp.httpheader;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;

/**
 * This class is used to check allowed senderId's when the property approve.to.use.header.original.consumer is set to true.
 * If that is the case, and if incoming request contains the header x-rivta-original-serviceconsumer-hsaid, the senderId must be
 * contained in the comma-separated property list sender.id.allowed.list
 */
@Log4j2
@Service
public class CheckSenderAllowedToUseHeaderImpl implements CheckSenderAllowedToUseHeader {

  private String[] senderIdArray;
  private String senderIdString;

  private static final String MSG_SENDERID_MISSING = "The sender was null/empty. Could not check address in list {}. HTTP header that caused checking: {}.";
  private static final String MSG_SENDER_ALLOWED_SET_HEADER = "Sender '{}' allowed to set x-rivta-original-serviceconsumer-hsaid";
  private static final String LIST_EMPTY_WARNING = "The list of approved senders, that can use header {} was empty or null. SenderId was {}";
  private static final String MSG_SENDER_NOT_ALLOWED_SET_HEADER = "Sender '{}' not allowed to set header {}, accepted senderId's in '{}': [{}]";

  @Autowired
  public CheckSenderAllowedToUseHeaderImpl(@Value("${" + PropertyConstants.SENDER_ID_ALLOWED_LIST + ":#{null}}") String senderAllowedList) {

    if (!StringUtils.isEmpty(senderAllowedList)) {
      senderIdArray = senderAllowedList.split(",");
      senderIdString = senderAllowedList;
    }
  }

  public boolean isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader(String senderId) {

    if (senderId == null || senderId.trim().isEmpty()) {
      log.warn(MSG_SENDERID_MISSING, PropertyConstants.SENDER_ID_ALLOWED_LIST, HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID);
      return false;
    }

    if (senderIdArray == null || senderIdArray.length == 0) {
      log.warn(LIST_EMPTY_WARNING, HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, senderId);
      return false;
    }

    for (String id : senderIdArray) {
      if (senderId.trim().startsWith(id.trim())) {
        log.info(MSG_SENDER_ALLOWED_SET_HEADER, senderId, PropertyConstants.SENDER_ID_ALLOWED_LIST);
        return true;
      }
    }
    log.warn(MSG_SENDER_NOT_ALLOWED_SET_HEADER, senderId, HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, PropertyConstants.SENDER_ID_ALLOWED_LIST, senderIdString);
    return false;
  }
}
