package se.skl.tp.vp.httpheader;

import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.certificate.HeaderCertificateHelper;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@Service
@Log4j2
public class HttpSenderIdExtractorProcessorImpl implements HttpSenderIdExtractorProcessor {

  private static final String ROUTE_DELIMITER = "#";
  private static final Pattern ROUTE_DELIMITER_PATTERN = Pattern.compile(ROUTE_DELIMITER);

  private final IPWhitelistHandler ipWhitelistHandler;
  private final HeaderCertificateHelper headerCertificateHelper;
  private final SenderIpExtractor senderIpExtractor;
  private final String vpInstanceId;
  private final ExceptionUtil exceptionUtil;
  private final boolean useRoutingHistory;

  @Autowired
  public HttpSenderIdExtractorProcessorImpl(Environment env,
      SenderIpExtractor senderIpExtractor,
      HeaderCertificateHelper headerCertificateHelper,
      IPWhitelistHandler ipWhitelistHandler,
      ExceptionUtil exceptionUtil) {
    this.headerCertificateHelper = headerCertificateHelper;
    this.ipWhitelistHandler = ipWhitelistHandler;
    this.senderIpExtractor = senderIpExtractor;
    this.vpInstanceId = env.getProperty(PropertyConstants.VP_INSTANCE_ID);
    this.useRoutingHistory = "true"
        .equals(env.getProperty(PropertyConstants.VP_USE_ROUTING_HISTORY));
    this.exceptionUtil = exceptionUtil;
  }

  @Value("${http.forwarded.header.auth_cert}")
  String authCertHeaderName;

  @Override
  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();

    String callerRemoteAddress = senderIpExtractor.getCallerRemoteAddress(message);
    checkCallerOnWhitelist(callerRemoteAddress,
        senderIpExtractor.getCallerRemoteAddressHeaderName());

    String forwardedForIpAdress = senderIpExtractor.getForwardedForAddress(message);
    String senderIpAdress =
        forwardedForIpAdress != null ? forwardedForIpAdress : callerRemoteAddress;
    exchange.setProperty(VPExchangeProperties.SENDER_IP_ADRESS, senderIpAdress);

    String senderId = message.getHeader(HttpHeaders.X_VP_SENDER_ID, String.class);
    String senderVpInstanceId = message.getHeader(HttpHeaders.X_VP_INSTANCE_ID, String.class);
    if (senderId != null && vpInstanceId.equals(senderVpInstanceId)) {
      log.debug("Internal plattform call, setting senderId from property {}:{}",
          HttpHeaders.X_VP_SENDER_ID, senderId);
      checkCallerOnWhitelist(forwardedForIpAdress, senderIpExtractor.getForwardForHeaderName());
      exchange.setProperty(VPExchangeProperties.SENDER_ID, senderId);
    } else {
      log.debug("Try extract senderId from provided certificate");
      exchange.setProperty(VPExchangeProperties.SENDER_ID, getSenderIdFromCertificate(message));
      if (useRoutingHistory) {
        handleRoutingHistory(message);
      }
    }
  }

  private void handleRoutingHistory(Message message) {
    String forwardedList = message.getHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, String.class);
    if (forwardedList == null || forwardedList.length() == 0) {
      forwardedList = message.getExchange().getProperty(VPExchangeProperties.SENDER_ID).toString();
    } else if (isInForwardedList(forwardedList)) {
      throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP014);
    }
    forwardedList += ROUTE_DELIMITER + vpInstanceId;

    message.setHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, forwardedList);
  }

  private boolean isInForwardedList(String s) {
    return ROUTE_DELIMITER_PATTERN.splitAsStream(s)
        .anyMatch(e -> e.trim().equals(vpInstanceId));
  }

  private String getSenderIdFromCertificate(Message message) {
    Object certificate = message.getHeader(authCertHeaderName);

    return headerCertificateHelper.getSenderIDFromHeaderCertificate(certificate);
  }

  private void checkCallerOnWhitelist(String senderIpAdress, String header) {
    if (senderIpAdress != null && !ipWhitelistHandler.isCallerOnWhiteList(senderIpAdress)) {
      throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP011,
          " IP-address: " + senderIpAdress
              + ". HTTP header that caused checking: " + header);
    }
  }
}
