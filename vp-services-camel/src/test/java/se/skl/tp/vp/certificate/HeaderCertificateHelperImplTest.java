package se.skl.tp.vp.certificate;


import io.undertow.util.FileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.skl.tp.vp.exceptions.VpSemanticException;

public class HeaderCertificateHelperImplTest {

  public static final String PRINCIPAL_OK = "CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB";
  public static final String PRINCIPAL_HEX_OU = "OU=#00074861726d6f6e79";
  public static final String PRINCIPAL_MISSING_OU = "CN=Hermione Granger, O=Apache Software Foundation, L=Hogwarts, ST=Hants, C=GB";
  final String pattern = "OU=([^,]+)";
  final String vpInstance = "NTjP Develop";
  HeaderCertificateHelperImpl headerCertificateHelper = new HeaderCertificateHelperImpl(pattern, vpInstance);

  @Test
  public void getSenderIDFromHeaderCertificate() {
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_OK));
    assertEquals("Harmony", senderId);
  }

  @Test
  public void getHexSenderIDFromHeaderCertificate() {
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_HEX_OU));
    assertEquals("Harmony", senderId);
  }

  @Test
  public void getSenderIDFromPemCertificate() {
    String pemCert = FileUtils.readFile(getClass().getClassLoader().getResource("certs/cert_ou_is_tp.pem"));
    String senderId = headerCertificateHelper.getSenderIDFromHeaderCertificate(pemCert);
    assertEquals("tp", senderId);
  }

  @Test
  public void wrongCertFormatShouldThrowVP002() {

    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(new X500Principal(""));
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals("VP002 [NTjP Develop] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad.", e.getMessage());
      assertEquals("Exception, unkown certificate type found in httpheader x-vp-auth-cert", e.getMessageDetails());
    }
  }

  @Test
  public void pemCertParseErrorShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(
          "-----BEGIN CERTIFICATE-----This string will cause a parse error-----END CERTIFICATE-----");
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals("VP002 [NTjP Develop] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad.", e.getMessage());
      assertEquals("Exception occured parsing certificate in httpheader x-vp-auth-cert", e.getMessageDetails());
    }
  }

  @Test
  public void nullCertShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(null);
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals("VP002 [NTjP Develop] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad.", e.getMessage());
      assertEquals("No certificate found in httpheader x-vp-auth-cert", e.getMessageDetails());
    }
  }

  @Test
  public void ifSenderIdNotFoundInCertItShouldThrowVP002() {
    try {
      headerCertificateHelper.getSenderIDFromHeaderCertificate(mockCert(PRINCIPAL_MISSING_OU));
      fail("Exception was not thrown when certificate OU missing");
    } catch (final VpSemanticException e) {
      assertEquals("No senderId found in Certificate", e.getMessageDetails());
      assertEquals("VP002 [NTjP Develop] Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad.", e.getMessage());
    }
  }

  private Object mockCert(String dnString) {

    final X509Certificate cert = Mockito.mock(X509Certificate.class);
    X500Principal principal = new X500Principal(dnString);
    Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
    return cert;
  }
}