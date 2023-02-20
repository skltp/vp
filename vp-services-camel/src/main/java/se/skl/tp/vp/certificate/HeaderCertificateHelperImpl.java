package se.skl.tp.vp.certificate;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


@Service
@Log4j2
public class HeaderCertificateHelperImpl implements HeaderCertificateHelper {

  final private SenderIdExtractor senderIdExtractor;
  private final String vpInstance;

  private static final String PATTERN_PROPERTY = "${" + PropertyConstants.CERTIFICATE_SENDERID_SUBJECT_PATTERN + "}";
  private static final String VP_INSTANCE = "${" + PropertyConstants.VP_INSTANCE_NAME + "}";

  private static final String USE_TREAFIK_SUBJECT_DN = "${" + PropertyConstants.VP_HEADER_AUTH_DN_TRAEFIK + "}";

  private static final String USE_APACHE_SUBJECT_DN =  "${" + PropertyConstants.VP_HEADER_AUTH_DN_APACHE + "}";

  private static final String AUTH_CERT_HEADER_NAME =  "${" + PropertyConstants.VP_HEADER_AUTH_CERTIFICATE + "}";

  private final String authCertHeaderName;
  private final boolean useTraefikPassTLSInfo;
  private final boolean useVPAuthDN;



  @Autowired
  public HeaderCertificateHelperImpl(@Value(PATTERN_PROPERTY) String certificateSenderId,
                                     @Value(VP_INSTANCE) String vpInstance,
                                     @Value(AUTH_CERT_HEADER_NAME) String authCertHeaderName,
                                     @Value(USE_APACHE_SUBJECT_DN) boolean useApacheSubjDN,
                                     @Value(USE_TREAFIK_SUBJECT_DN) boolean useTraefikSubjectDN) {
    senderIdExtractor = new SenderIdExtractor(certificateSenderId);
    this.vpInstance = vpInstance;
    this.authCertHeaderName = authCertHeaderName;
    this.useVPAuthDN = useApacheSubjDN;
    this.useTraefikPassTLSInfo = useTraefikSubjectDN;
  }


  public String getSenderIDFromHeader(Message message) {
    String senderId = null;
    if (useVPAuthDN) {
      String header = message.getHeader(HttpHeaders.X_VP_AUTH_DN, String.class);
      if (header != null) {
        senderId = senderIdExtractor.extractSenderFromPrincipal(header);
      }
    }
    if (useTraefikPassTLSInfo) {
      String header = message.getHeader(HttpHeaders.X_FORWARDED_TLS_CLIENT_CERT_INFO, String.class);
      if (header != null) {
        senderId = senderIdExtractor.extractSenderFromPrincipal(header);
      }
    }
    return senderId;
  }
  public String getSenderIDFromHeaderCertificate(Object certificate) {
    String senderId = null;
    boolean isUnknownCertificateType = false;

    evaluateCertificateNotNull(certificate);

    try {
      if (isX509Certificate(certificate, authCertHeaderName)) {
        senderId = extractFromX509Certificate(certificate);
      } else if (PemConverter.isPEMCertificate(certificate)) {
        senderId = extractFromPemFormatCertificate(certificate);
      } else {
        isUnknownCertificateType = true;
      }
    } catch (Exception e) {
      log.error("Error occured parsing certificate in httpheader: {}", authCertHeaderName, e);
      throw createVP002Exception("Exception occured parsing certificate in httpheader "
          + authCertHeaderName);
    }

    evaluateResult(senderId, isUnknownCertificateType);
    return senderId;
  }

  private void evaluateCertificateNotNull(Object certificate) {
    if (certificate == null) {
      throw createVP002Exception("No certificate found in httpheader "
          + authCertHeaderName);
    }
  }

  private void evaluateResult(String senderId, boolean isUnknownCertificateType) {
    if (isUnknownCertificateType) {
      throw createVP002Exception("Exception, unkown certificate type found in httpheader "
          + authCertHeaderName);

    } else if (senderId == null) {
      throw createVP002Exception("No senderId found in Certificate");
    }
  }

  private String extractFromPemFormatCertificate(Object certificate) throws CertificateException {
    X509Certificate x509Certificate = PemConverter.buildCertificate(certificate);
    return extractSenderIdFromCertificate(x509Certificate);
  }

  private String extractFromX509Certificate(Object certificate) {
    X509Certificate x509Certificate = (X509Certificate) certificate;
    return extractSenderIdFromCertificate(x509Certificate);
  }

  private static boolean isX509Certificate(Object certificate, String headerName) {
    if (certificate instanceof X509Certificate) {
      log.debug("Found X509Certificate in httpheader: {}", headerName);
      return true;
    }
    return false;
  }

  private String extractSenderIdFromCertificate(final X509Certificate certificate) {
    log.debug("Extracting sender id from certificate.");
    final String principalName = certificate.getSubjectX500Principal().getName();
    return senderIdExtractor.extractSenderFromPrincipal(principalName);
  }

  private VpSemanticException createVP002Exception(String details) {

    return new VpSemanticException(VpSemanticErrorCodeEnum.VP002,
        VpSemanticErrorCodeEnum.VP002 +" [" + vpInstance + "] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad." ,
        details);
  }
}
