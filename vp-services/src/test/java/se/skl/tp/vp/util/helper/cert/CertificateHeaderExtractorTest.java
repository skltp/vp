package se.skl.tp.vp.util.helper.cert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

public class CertificateHeaderExtractorTest {

	/**
	 * Test that we can extract a certificate when it comes in the http header.
	 * Reverse proxy mode
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractX509CertificateCertificateFromHeader() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, null, "192.168.0.109");
		final X509Certificate certificate = helper.extractCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);

		assertNotNull(certificate);
	}

	@Test
	public void testExtractX509CertificateCertificateFromHeaderAndInWhiteList() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();
		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, null,
				"192.168.0.109, 127.0.0.1, localhost");
		helper.extractCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}

	@Test
	public void testExtractX509CertificateCertificateFromHeaderAndNotInWhiteList() throws Exception {

		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, null,
				"192.168.0.108, 127.0.0.1, localhost");
		try {
			helper.extractCertificate();

			fail("Exception not thrown when caller was not in the ip white list");
		} catch (final VpSemanticException e) {
			// OK
			assertEquals("Caller was not on the white list of accepted IP-addresses.", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}

	@Test
	public void testExtractX509CertificateCertificateWithSingleWhiteListEntry() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, null, "192.168.0.109");
		helper.extractCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}

	/**
	 * Test that we get a VPSemantic exception when we have a different kind of
	 * certificate in the header
	 */
	@Test
	public void testExtractUnkownCertificateTypeFromHeader() {

		final Certificate cert = Mockito.mock(Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("/127.0.0.1:12345");

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, null, "127.0.0.1");
		try {
			helper.extractCertificate();

			fail("No exception thrown when certificate was of wrong type");
		} catch (final VpSemanticException e) {
			assertEquals("VP002 Exception occured parsing certificate in httpheader x-vp-auth-cert", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
	}

	@Test
	public void extractPEMCertificate() throws Exception {
		String pemCertContent = readPemCertificateFile();
		final X509Certificate certificate = CertificateHeaderExtractor.buildCertificateFromPem(pemCertContent);
		assertNotNull(certificate);
	}

	@Test(expected = CertificateException.class)
	public void extractIncorrectPEMCertificateShouldThrowException() throws Exception {
		String pemCertContent = "-----BEGIN CERTIFICATE-----Incorrect CERT string-----END CERTIFICATE-----";
		CertificateHeaderExtractor.buildCertificateFromPem(pemCertContent);
		fail("Expected CertificateException when incorrect pem certificate");
	}

	@Test
	public void commonNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
		String pemCertContent = readPemCertificateFile();

		final X509Certificate certificate = CertificateHeaderExtractor.buildCertificateFromPem(pemCertContent);
		final X509Principal issuer = PrincipalUtil.getIssuerX509Principal(certificate);
		final String cn = (String) issuer.getValues(X509Name.CN).get(0);

		assertEquals("SITHS CA v3", cn);
	}

	@Test
	public void organizationUnitNameIsCorrectWhenExtractingPEMCertificate() throws Exception {
		String pemCertContent = readPemCertificateFile();

		final X509Certificate certificate = CertificateHeaderExtractor.buildCertificateFromPem(pemCertContent);
		final X509Principal subject = PrincipalUtil.getSubjectX509Principal(certificate);
		final String organizationalUnitName = (String) subject.getValues(X509Name.OU).get(0);

		assertEquals("VP", organizationalUnitName);
	}

	private String readPemCertificateFile() throws IOException {
		URL filePath = CertificateHeaderExtractorTest.class.getClassLoader().getResource("certs/client.pem");
		File file = FileUtils.toFile(filePath);
		String pemCertContent = FileUtils.readFileToString(file);
		return pemCertContent;
	}

	private MuleMessage mockCertAndRemoteAddress() {
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("/192.168.0.109:12345");
		return msg;
	}

}
