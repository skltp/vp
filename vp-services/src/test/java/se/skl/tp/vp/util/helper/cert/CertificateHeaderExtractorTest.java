package se.skl.tp.vp.util.helper.cert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

public class CertificateHeaderExtractorTest {

	private Pattern pattern = Pattern.compile("OU" + VPUtil.CERT_SENDERID_PATTERN);

	/**
	 * Test that we can extract a certificate when it comes in the http header.
	 * Reverse proxy mode
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractX509CertificateCertificateFromHeader() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "192.168.0.109");
		final String senderId = helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);

		assertNotNull(senderId);
		assertEquals("Harmony", senderId);
	}

	@Test
	public void testExtractX509CertificateCertificateFromHeaderAndInWhiteList() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();
		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern,
				"192.168.0.109, 127.0.0.1, localhost");
		helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}

	@Test
	public void testExtractX509CertificateCertificateFromHeaderAndNotInWhiteList() throws Exception {

		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern,
				"192.168.0.108, 127.0.0.1, localhost");
		try {
			helper.extractSenderIdFromCertificate();

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

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "192.168.0.109");
		helper.extractSenderIdFromCertificate();

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

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "127.0.0.1");
		try {
			helper.extractSenderIdFromCertificate();

			fail("No exception thrown when certificate was of wrong type");
		} catch (final VpSemanticException e) {
			assertEquals("VP002 Exception occured parsing certificate in httpheader x-vp-auth-cert", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
	}

	@Test
	public void extractSenderIdfromPlainX500PrincipalStringWithSlashes() {

		String cert = "/C=se/DC=Services/DC=Nod1/O=165565594230/OU=TP/CN=tk_qa.tp.cybercom.carelink.sjunet.org/serialNumber=HSASERVICES-100M";

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("/192.168.0.109:12345");

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "192.168.0.109");
		final String senderId = helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);

		assertNotNull(senderId);
		assertEquals("TP", senderId);
	}

	@Test
	public void extractSenderIdfromPlainX500PrincipalStringWithComma() {
		String cert = "C=se,DC=Services,DC=Nod1,O=165565594230,OU=TP,CN=tk_qa.tp.cybercom.carelink.sjunet.org,serialNumber=HSASERVICES-100M";

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("/192.168.0.109:12345");

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "192.168.0.109");
		final String senderId = helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);

		assertNotNull(senderId);
		assertEquals("TP", senderId);
	}

	private MuleMessage mockCertAndRemoteAddress() {

		X500Principal principal = new X500Principal(
				"CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB");

		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("/192.168.0.109:12345");

		return msg;
	}

}
