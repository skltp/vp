package se.skl.tp.vp.util.helper;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

public class CertificateHelperUnitTest extends TestCase {

	/**
	 * Test that we can extract a certificate when
	 * it comes in the http header. Reverse proxy mode
	 * @throws Exception
	 */
	public void testExtractCertificateFromHeader() throws Exception {
	
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		final X509Certificate certificate = helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		
		assertNotNull(certificate);
	}
	
	/**
	 * Test that we get a VPSemantic exception when we have a
	 * different kind of certificate in the header
	 */
	public void testExtractNoX509CertificateFromHeader() {
		
		final Certificate cert = Mockito.mock(Certificate.class);
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		try { 
			helper.extractCertificate();
			
			fail("No exception thrown when certificate was of wrong type");
		} catch (final VpSemanticException e) {
			assertEquals("VP002 Exception caught when trying to extract certificate from VP_CERTIFICATE http header. Expected a X509Certificate", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
	}
	
	/**
	 * Test that we can extract a certificate that
	 * is in the mule message. Regular mode.
	 */
	public void testExtractCertificateFromChain() {
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		
		final Certificate[] certs = new Certificate[1];
		certs[0] = cert;
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.PEER_CERTIFICATES)).thenReturn(certs);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		final X509Certificate certificate = helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		
		assertNotNull(certificate);
	}
	
	public void testExtractCertificateWhenChainIsNull() throws Exception {
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		try {
			helper.extractCertificate();
			
			fail("Exception was not thrown when certificate chain was null");
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 no certificates. The certificate chain was null", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
	}
	
	public void testExtractNoX509CertificateFromChain() throws Exception {
		
		final Certificate cert = Mockito.mock(Certificate.class);
		final Certificate[] certs = new Certificate[1];
		
		certs[0] = cert;
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.PEER_CERTIFICATES)).thenReturn(certs);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		
		try {
			helper.extractCertificate();
			
			fail("No exception was thrown when certificate in cert chain was of wrong type");
			
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
	}
	
	public void testExtractSenderFromCertificate() throws Exception {
		
		final X500Principal principal = new X500Principal("OU=marcus");
		
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		final Pattern pattern = Pattern.compile("OU=([^,]+)");
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		final String sender = helper.extractSenderIdFromCertificate(cert, pattern);
		
		assertNotNull(sender);
		assertEquals("marcus", sender);
		
		/*
		 * Verifications
		 */
		Mockito.verify(cert, Mockito.times(1)).getSubjectX500Principal();
	}
	
	public void testExtractSenderWithNullCert() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateHelper helper = new CertificateHelper(msg);
		
		try {
			helper.extractSenderIdFromCertificate(null, null);
			fail("Exception not thrown when certificate was null");
		} catch (final NullPointerException e) {
			assertEquals("Cannot extract any sender because the certificate was null", e.getMessage());
		}
		
		Mockito.verifyZeroInteractions(msg);
	}
	
	public void testExtractSenderWithNullPattern() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateHelper helper = new CertificateHelper(msg);
		
		try {
			helper.extractSenderIdFromCertificate(Mockito.mock(X509Certificate.class), null);
			fail("No exception was thrown when pattern was null");
		} catch (final NullPointerException e) {
			assertEquals("Cannot extract any sender becuase the pattern used to find it was null", e.getMessage());
		}
		
		Mockito.verifyZeroInteractions(msg);
	}
	
	public void testNoSenderInCertificate() {
		final X500Principal principal = new X500Principal("CN=marcus");
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		final Pattern pattern = Pattern.compile("OU=([^,]+)");
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final CertificateHelper helper = new CertificateHelper(msg);
		
		try {
			helper.extractSenderIdFromCertificate(cert, pattern);
			fail("Did not expect a sender id in certificate");
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 No senderId found in Certificate: " + principal.getName(), e.getMessage());
		}
		
		Mockito.verify(cert, Mockito.times(1)).getSubjectX500Principal();
		Mockito.verifyZeroInteractions(msg);
	}
	
}
