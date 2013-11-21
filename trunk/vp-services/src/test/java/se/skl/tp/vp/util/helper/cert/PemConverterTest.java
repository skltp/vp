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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.junit.Test;

public class PemConverterTest {

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

	private String readPemCertificateFile(String pemFile) throws IOException {
		URL filePath = CertificateHeaderExtractorTest.class.getClassLoader().getResource(pemFile);
		File file = FileUtils.toFile(filePath);
		String pemCertContent = FileUtils.readFileToString(file);
		return pemCertContent;
	}

}
