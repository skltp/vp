package se.skl.tp.vp.certificate;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.utils.FileUtils;

public class HeaderCertificateHelperImplTest {

  public static final String PRINCIPAL_OK = "CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB";
  public static final String PRINCIPAL_HEX_OU = "OU=#00074861726d6f6e79";
  public static final String PRINCIPAL_MISSING_OU = "CN=Hermione Granger, O=Apache Software Foundation, L=Hogwarts, ST=Hants, C=GB";
  final String pattern = "(?:OU|2.5.4.5|SERIALNUMBER)=([^,]+)";
  final String vpInstance = "NTjP Develop";
  HeaderCertificateHelperImpl headerCertificateHelper = new HeaderCertificateHelperImpl(pattern, vpInstance);

  private static final String traefikCertHeader = "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";
  private static final String traefikCertHeader_senderId = "TSTNMT2321000156-B02";
  private static final String vp002_message = "VP002 [NTjP Develop] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad.";

  @Value("${http.forwarded.header.auth_cert}")
  private String authCertName;

  @Test
  public void getSenderIDFromHeaderCertificate() {
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_OK));
    assertEquals("Harmony", senderId);
  }

  @Test
  public void getSenderIDFromTraefikHeaderCertificate() {
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(traefikCertHeader);
    assertEquals(traefikCertHeader_senderId, senderId);
  }


  @Test
  public void getHexSenderIDFromHeaderCertificate() {
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_HEX_OU));
    assertEquals("Harmony", senderId);
  }

  @Test
  public void getSenderIDFromPemCertificate() {
    URL url = getClass().getClassLoader().getResource("certs/cert_ou_is_tp.pem");
    String pemCert = FileUtils.readFile(url);
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(pemCert);
    assertEquals("tp", senderId);
  }

  @Test
  public void wrongCertFormatShouldThrowVP002() {

    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(new X500Principal(""));
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals(vp002_message, e.getMessage());
      String expectedMessage = "Exception, unkown certificate type found in httpheader " + authCertName;
      assertEquals(expectedMessage, e.getMessageDetails());
    }
  }

  @Test
  public void pemCertParseErrorShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(
          "-----BEGIN CERTIFICATE-----This string will cause a parse error-----END CERTIFICATE-----");
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals(vp002_message, e.getMessage());
      String expectedMessage = "Exception occured parsing certificate in httpheader " + authCertName;
      assertEquals(expectedMessage, e.getMessageDetails());
    }
  }

  @Test
  public void nullCertShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(null);
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals(vp002_message, e.getMessage());
      String expectedMessage = "No certificate found in httpheader " + authCertName;
      assertEquals(expectedMessage, e.getMessageDetails());
    }
  }

  @Test
  public void ifSenderIdNotFoundInCertItShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_MISSING_OU));
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals("No senderId found in Certificate", e.getMessageDetails());
      assertEquals(vp002_message, e.getMessage());
    }
  }

  private Object mockCert(String dnString) {

    final X509Certificate cert = Mockito.mock(X509Certificate.class);
    X500Principal principal = new X500Principal(dnString);
    Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
    return cert;
  }
}