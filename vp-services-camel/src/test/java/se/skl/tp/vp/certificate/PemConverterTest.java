package se.skl.tp.vp.certificate;

import io.undertow.util.FileUtils;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.junit.Test;

import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class PemConverterTest  {

    @Test
    public void extractPEMCertificate() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/client.pem");
        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        assertNotNull(certificate);
    }

    @Test
    public void extractPEMCertificateIncludingWhiteSpaces() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/clientPemWithWhiteSpaces.pem");
        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        assertNotNull(certificate);
    }

    @Test(expected = CertificateException.class)
    public void extractIncorrectPEMCertificateShouldThrowException() throws Exception {
        String pemCertContent = "-----BEGIN CERTIFICATE-----Incorrect CERT string-----END CERTIFICATE-----";
        PemConverter.buildCertificate(pemCertContent);
        fail("Expected CertificateException when incorrect pem certificate");
    }

    @Test
    public void commonNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/client.pem");

        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        final X509Principal issuer = PrincipalUtil.getIssuerX509Principal(certificate);
        final String cn = (String) issuer.getValues(X509Name.CN).get(0);

        assertEquals("SITHS CA v3", cn);
    }

    @Test
    public void organizationUnitNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/client.pem");

        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        final X509Principal subject = PrincipalUtil.getSubjectX509Principal(certificate);
        final String organizationalUnitName = (String) subject.getValues(X509Name.OU).get(0);

        assertEquals("VP", organizationalUnitName);
    }

    private String readPemCertificateFile(String pemFile) {
        URL filePath = PemConverterTest.class.getClassLoader().getResource(pemFile);
        String pemCertContent = FileUtils.readFile(filePath);
        return pemCertContent;
    }
}
