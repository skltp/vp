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
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("127.0.0.1");
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "127.0.0.1");
		final X509Certificate certificate = helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		
		assertNotNull(certificate);
	}
	
	public void testExtractCertificateFromHeaderAndInWhiteList() throws Exception {
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("192.168.0.109");
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "192.168.0.109, 127.0.0.1, localhost");
		helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}
	
	public void testExtractCertificateFromHeaderAndNotInWhiteList() throws Exception {
		
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("192.168.0.109");
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "192.168.0.108, 127.0.0.1, localhost");
		try {
			helper.extractCertificate();
			
			fail("Exception not thrown when caller was not in the ip white list");
		} catch (final VpSemanticException e) {
			// OK
			assertEquals("Caller was not on the white list of accepted IP-addresses.", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}
	
	public void testExtractCertificateWithSingleWhiteListEntry() throws Exception {
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("192.168.0.109");
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "192.168.0.109");
		helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES);
	}
	
	/**
	 * Test that we get a VPSemantic exception when we have a
	 * different kind of certificate in the header
	 */
	public void testExtractNoX509CertificateFromHeader() {
		
		final Certificate cert = Mockito.mock(Certificate.class);
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR)).thenReturn("127.0.0.1");
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "127.0.0.1");
		try { 
			helper.extractCertificate();
			
			fail("No exception thrown when certificate was of wrong type");
		} catch (final VpSemanticException e) {
			assertEquals("VP002 Exception caught when trying to extract certificate from VP_CERTIFICATE http header. Expected a X509Certificate", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(2)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR);
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
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "127.0.0.1");
		final X509Certificate certificate = helper.extractCertificate();
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);
		
		assertNotNull(certificate);
	}
	
	public void testExtractCertificateWhenChainIsNull() throws Exception {
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "127.0.0.1");
		try {
			helper.extractCertificate();
			
			fail("Exception was not thrown when certificate chain was null");
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 no certificates. The certificate chain was null", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);
	}
	
	public void testExtractNoX509CertificateFromChain() throws Exception {
		
		final Certificate cert = Mockito.mock(Certificate.class);
		final Certificate[] certs = new Certificate[1];
		
		certs[0] = cert;
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.PEER_CERTIFICATES)).thenReturn(certs);
		
		final CertificateHelper helper = new CertificateHelper(msg, null, "127.0.0.1");
		
		try {
			helper.extractCertificate();
			
			fail("No exception was thrown when certificate in cert chain was of wrong type");
			
		} catch (final VpSemanticException e) {
			// Ok
			assertEquals("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate", e.getMessage());
		}
		
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.PEER_CERTIFICATES);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.REMOTE_ADDR);
	}
	
	public void testExtractSenderFromCertificate() throws Exception {
		
		final X500Principal principal = new X500Principal("OU=marcus");
		
		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		final Pattern pattern = Pattern.compile("OU=([^,]+)");
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final CertificateHelper helper = new CertificateHelper(msg, pattern, "127.0.0.1");
		final String sender = helper.extractSenderIdFromCertificate(cert);
		
		assertNotNull(sender);
		assertEquals("marcus", sender);
		
		/*
		 * Verifications
		 */
		Mockito.verify(cert, Mockito.times(1)).getSubjectX500Principal();
	}
	
	public void testExtractSenderWithNullCert() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateHelper helper = new CertificateHelper(msg, null, null);
		
		try {
			helper.extractSenderIdFromCertificate(null);
			fail("Exception not thrown when certificate was null");
		} catch (final NullPointerException e) {
			assertEquals("Cannot extract any sender because the certificate was null", e.getMessage());
		}
		
		Mockito.verifyZeroInteractions(msg);
	}
	
	public void testExtractSenderWithNullPattern() throws Exception {
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		final CertificateHelper helper = new CertificateHelper(msg, null, null);
		
		try {
			helper.extractSenderIdFromCertificate(Mockito.mock(X509Certificate.class));
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
		
		final CertificateHelper helper = new CertificateHelper(msg, pattern, null);
		
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
