package se.skl.tp.vp.certificate;

import io.undertow.util.FileUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    public void extractIncorrectPEMCertificateShouldThrowException() throws Exception {
        assertThrows(CertificateException.class, () -> {
            String pemCertContent = "-----BEGIN CERTIFICATE-----Incorrect CERT string-----END CERTIFICATE-----";
            PemConverter.buildCertificate(pemCertContent);
        });
    	//fail("Expected CertificateException when incorrect pem certificate");

    }

    @Test
    public void commonNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/client.pem");

        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        final X500Name issuer = new JcaX509CertificateHolder(certificate).getIssuer();
        final String cn = IETFUtils.valueToString(issuer.getRDNs(BCStyle.CN)[0].getFirst().getValue());

        assertEquals("SITHS CA v3", cn);
    }

    @Test
    public void organizationUnitNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
        String pemCertContent = readPemCertificateFile("certs/client.pem");

        final X509Certificate certificate = PemConverter.buildCertificate(pemCertContent);
        final X500Name subject = new JcaX509CertificateHolder(certificate).getSubject();
        final String organizationalUnitName = IETFUtils.valueToString(subject.getRDNs(BCStyle.OU)[0].getFirst().getValue());

        assertEquals("VP", organizationalUnitName);
    }

    private String readPemCertificateFile(String pemFile) {
        URL filePath = PemConverterTest.class.getClassLoader().getResource(pemFile);
        assertNotNull(filePath);
        return FileUtils.readFile(filePath);
    }
}
