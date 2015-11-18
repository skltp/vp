/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
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
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.HttpHeaders;
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

		Mockito.verify(msg, Mockito.times(1)).getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND);

		assertNotNull(senderId);
		assertEquals("Harmony", senderId);
	}

	@Test
	public void testExtractX509CertificateCertificateFromHeaderAndInWhiteList() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();
		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern,
				"192.168.0.109, 127.0.0.1, localhost");
		helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES, PropertyScope.OUTBOUND);
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
			assertEquals("VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 192.168.0.109. HTTP header that caused checking: x-vp-auth-cert", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(0)).getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES, PropertyScope.OUTBOUND);
	}

	@Test
	public void testExtractX509CertificateCertificateWithSingleWhiteListEntry() throws Exception {
		final MuleMessage msg = mockCertAndRemoteAddress();

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "192.168.0.109");
		helper.extractSenderIdFromCertificate();

		Mockito.verify(msg, Mockito.times(1)).getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(0)).getProperty(VPUtil.PEER_CERTIFICATES, PropertyScope.OUTBOUND);
	}

	/**
	 * Test that we get a VPSemantic exception when we have a different kind of
	 * certificate in the header
	 */
	@Test
	public void testExtractUnkownCertificateTypeFromHeader() {

		final Certificate cert = Mockito.mock(Certificate.class);
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND)).thenReturn("/127.0.0.1:12345");

		final CertificateHeaderExtractor helper = new CertificateHeaderExtractor(msg, pattern, "127.0.0.1");
		try {
			helper.extractSenderIdFromCertificate();

			fail("No exception thrown when certificate was of wrong type");
		} catch (final VpSemanticException e) {
			assertEquals("VP002 Exception occured parsing certificate in httpheader x-vp-auth-cert", e.getMessage());
		}

		Mockito.verify(msg, Mockito.times(1)).getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);
		Mockito.verify(msg, Mockito.times(1)).getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND);
	}

	private MuleMessage mockCertAndRemoteAddress() {

		X500Principal principal = new X500Principal(
				"CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB");

		final X509Certificate cert = Mockito.mock(X509Certificate.class);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);

		final DefaultMuleMessage msg = Mockito.mock(DefaultMuleMessage.class);
		Mockito.when(msg.getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND)).thenReturn(cert);
		Mockito.when(msg.getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND)).thenReturn("/192.168.0.109:12345");

		return msg;
	}

}
