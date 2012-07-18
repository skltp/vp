package se.skl.tp.vp.util.helper.cert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vp.exceptions.VpSemanticException;

public class CertificateExtractorBaseTest {

	@Test
	public void testExtractSenderFromCertificate() throws Exception {

		final X500Principal principal = new X500Principal("OU=marcus");

		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);

		final Pattern pattern = Pattern.compile("OU=([^,]+)");

		final MuleMessage msg = Mockito.mock(MuleMessage.class);

		final CertificateExtractorBase helper = new CertificateExtractorBase(msg, pattern, "127.0.0.1");
		final String sender = helper.extractSenderIdFromCertificate(cert);

		assertNotNull(sender);
		assertEquals("marcus", sender);

		/*
		 * Verifications
		 */
		Mockito.verify(cert, Mockito.times(1)).getSubjectX500Principal();
	}

	@Test
	public void testExtractSenderFromCertificateInHexMode() throws Exception {
		final String sender = "#131048534153455256494345532d31303358";

		final X500Principal principal = new X500Principal("OU=" + sender);

		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);

		final Pattern pattern = Pattern.compile("OU=([^,]+)");

		final MuleMessage msg = Mockito.mock(MuleMessage.class);

		final CertificateExtractorBase helper = new CertificateExtractorBase(msg, pattern, "127.0.0.1");
		final String s = helper.extractSenderIdFromCertificate(cert);

		System.out.println("Sender: " + s);

		assertNotNull(s);

	}

	@Test
	public void testExtractSenderWithNullCert() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateExtractorBase helper = new CertificateExtractorBase(msg, null, null);

		try {
			helper.extractSenderIdFromCertificate(null);
			fail("Exception not thrown when certificate was null");
		} catch (final IllegalArgumentException e) {
			assertEquals("Cannot extract any sender because the certificate was null", e.getMessage());
		}

		Mockito.verifyZeroInteractions(msg);
	}

	@Test
	public void testExtractSenderWithNullPattern() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateExtractorBase helper = new CertificateExtractorBase(msg, null, null);

		try {
			helper.extractSenderIdFromCertificate(Mockito.mock(X509Certificate.class));
			fail("No exception was thrown when pattern was null");
		} catch (final IllegalArgumentException e) {
			assertEquals("Cannot extract any sender becuase the pattern used to find it was null", e.getMessage());
		}

		Mockito.verifyZeroInteractions(msg);
	}

	@Test
	public void testNoSenderInCertificate() {
		final X500Principal principal = new X500Principal("CN=marcus");
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);

		final Pattern pattern = Pattern.compile("OU=([^,]+)");

		final MuleMessage msg = Mockito.mock(MuleMessage.class);

		final CertificateExtractorBase helper = new CertificateExtractorBase(msg, pattern, null);

		try {
			helper.extractSenderIdFromCertificate(cert);
			fail("Did not expect a sender id in certificate");
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 No senderId found in Certificate: " + principal.getName(), e.getMessage());
		}

		Mockito.verify(cert, Mockito.times(1)).getSubjectX500Principal();
		Mockito.verifyZeroInteractions(msg);
	}

}