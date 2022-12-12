package se.skl.tp.vp.certificate;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import lombok.extern.log4j.Log4j2;
import se.skl.tp.vp.constants.HttpHeaders;

/**
 * Generated a X509Certificate from e pem-string
 */
@Log4j2
public class PemConverter {

  private static final String BEGIN_HEADER = "-----BEGIN CERTIFICATE-----";
  private static final String END_HEADER = "-----END CERTIFICATE-----";

  private PemConverter() {
    // Static utility class
  }

  /*
   * PEM - (Privacy Enhanced Mail) Base64 encoded DER certificate, enclosed
   * between "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----"
   */
  public static X509Certificate buildCertificate(Object pemCert) throws CertificateException {

    String pemCertString = (String) pemCert;
    log.debug("Pem certificate in: {}", pemCertString);

    InputStream certificateInfo = extractCerticate(pemCertString);
    Certificate certificate = generateX509Certificate(certificateInfo);

    log.debug("Certificate converted to X509Certificate!");
    log.debug("Certificate principalname: {}", ((X509Certificate) certificate).getSubjectX500Principal().getName());
    return (X509Certificate) certificate;
  }

  private static Certificate generateX509Certificate(InputStream is) throws CertificateException {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    return factory.generateCertificate(is);
  }

  private static BufferedInputStream extractCerticate(String pemCertString) {

    StringBuilder formattedCert = new StringBuilder();

    if (pemCertString.startsWith("MII")) {
      String decoded = null;
      try {
        decoded = URLDecoder.decode(pemCertString, "utf8");

        formattedCert.append(BEGIN_HEADER);
        formattedCert.append("\n");
        formattedCert.append(decoded.split(",", 2)[0]);
        formattedCert.append("\n");
        formattedCert.append(END_HEADER);
        InputStream is = new ByteArrayInputStream(formattedCert.toString().getBytes());
        return new BufferedInputStream(is);
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }

    int beginHeader = pemCertString.indexOf(BEGIN_HEADER) + BEGIN_HEADER.length();
    int endHeader = pemCertString.indexOf(END_HEADER);

    formattedCert.append(BEGIN_HEADER);
    formattedCert.append("\n");
    formattedCert.append(pemCertString.substring(beginHeader, endHeader).replaceAll("\\s+", ""));
    formattedCert.append("\n");
    formattedCert.append(END_HEADER);

    pemCertString = formattedCert.toString();

    InputStream is = new ByteArrayInputStream((pemCertString).getBytes());
    return new BufferedInputStream(is);
  }

  public static boolean isPEMCertificate(Object certificate) {
    if (certificate instanceof String && containsCorrectPemHeaders((String) certificate)) {
      log.debug("Found possible PEM-encoded certificate in httpheader {{http.forwarded.header.auth_cert}}");
      return true;
    }
    return false;
  }

  private static boolean containsCorrectPemHeaders(String pemCertString) {
    int beginHeader = pemCertString.indexOf(BEGIN_HEADER);
    int endHeader = pemCertString.indexOf(END_HEADER);
    if (beginHeader != -1 && endHeader != -1) {
      return true;
    }
    else {
      if (pemCertString.startsWith("MII")) {
        return true;
      }
    }
    return false;

  }

}
