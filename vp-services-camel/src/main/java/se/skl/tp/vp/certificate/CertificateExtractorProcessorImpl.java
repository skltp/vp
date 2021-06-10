package se.skl.tp.vp.certificate;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.component.netty.NettyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
@Log4j2
public class CertificateExtractorProcessorImpl implements CertificateExtractorProcessor {


  SenderIdExtractor senderIdExtractor;
  private static final String PATTERN_PROPERTY = "${" + PropertyConstants.CERTIFICATE_SENDERID_SUBJECT_PATTERN + "}";

  @Autowired
  public CertificateExtractorProcessorImpl(@Value(PATTERN_PROPERTY) String certificateSenderidSubject) {
    senderIdExtractor = new SenderIdExtractor(certificateSenderidSubject);

  }

  @Override
  public void process(Exchange exchange){
    String principal = ""+exchange.getIn().getHeader(NettyConstants.NETTY_SSL_CLIENT_CERT_SUBJECT_NAME, String.class);
    String senderId = senderIdExtractor.extractSenderFromPrincipal(principal);

    if (senderId == null) {

      // todo NTP-1944 det ska vara bra om vi ska använda ExceptionUtil.createVpSemanticException för generera VpSemanticException, men jag
      // kan inte @Autowired ExceptionUtil. Det behövs att reda på varför
      // message och message details är felaktiga nu
      throw new VpSemanticException(VpSemanticErrorCodeEnum.VP002, VpSemanticErrorCodeEnum.VP002 + "", VpSemanticErrorCodeEnum.VP002 + "No senderId found in Certificate: " + principal);
    }

    exchange.setProperty(VPExchangeProperties.SENDER_ID, senderId);

  }


}
