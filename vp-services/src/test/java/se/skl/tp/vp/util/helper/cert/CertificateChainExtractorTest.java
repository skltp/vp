package se.skl.tp.vp.util.helper.cert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

public class CertificateChainExtractorTest {

	/**
	 * Test that we can extract a certificate that is in the mule message.
	 * Regular mode.
	 */
	@Test
	public void testExtractX509CertificateCertificateFromChain() {
		final X509Certificate cert = Mockito.mock(X509Certificate.class);

		final Certificate[] certs = new Certificate[1];
		certs[0] = cert;

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.PEER_CERTIFICATES)).thenReturn(certs);

		final CertificateChainExtractor helper = new CertificateChainExtractor(msg, null, "127.0.0.1");
		final X509Certificate certificate = helper.extractCertificate();

		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);

		assertNotNull(certificate);
	}

	@Test
	public void testExtractCertificateWhenChainIsNull() throws Exception {

		final MuleMessage msg = Mockito.mock(MuleMessage.class);

		final CertificateChainExtractor helper = new CertificateChainExtractor(msg, null, "127.0.0.1");
		try {
			helper.extractCertificate();

			fail("Exception was not thrown when certificate chain was null");
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 no certificates. The certificate chain was null", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);
	}

	@Test
	public void testExtractNoX509CertificateFromChain() throws Exception {

		final Certificate cert = Mockito.mock(Certificate.class);
		final Certificate[] certs = new Certificate[1];

		certs[0] = cert;

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.PEER_CERTIFICATES)).thenReturn(certs);

		final CertificateChainExtractor helper = new CertificateChainExtractor(msg, null, "127.0.0.1");

		try {
			helper.extractCertificate();

			fail("No exception was thrown when certificate in cert chain was of wrong type");

		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate",
					e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);
	}

}
