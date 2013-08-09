/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;

/**
 * 
 * Generated a X509Certificate from e pem-string
 * 
 */
public class PemConverter {

	private static final String BEGIN_HEADER = "-----BEGIN CERTIFICATE-----";
	private static final String END_HEADER = "-----END CERTIFICATE-----";

	private static Logger log = LoggerFactory.getLogger(PemConverter.class);

	/*
	 * PEM - (Privacy Enhanced Mail) Base64 encoded DER certificate, enclosed
	 * between "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----"
	 */
	static X509Certificate buildCertificate(Object pemCert) throws CertificateException {

		if (log.isDebugEnabled()) {
			log.debug("Got possible PEM-encoded certificate");
			log.debug((String) pemCert);
		}

		String pemCertString = (String) pemCert;

		if (containsCorrectPemHeaders(pemCertString)) {

			InputStream certificateInfo = extractCerticate(pemCertString);
			Certificate certificate = generateCertificate(certificateInfo);

			if (certificate instanceof X509Certificate) {
				log.debug("Certificate converted to X509Certificate!");
				log.debug("Certificate principalname: "
						+ ((X509Certificate) certificate).getSubjectX500Principal().getName());
				return (X509Certificate) certificate;
			} else {
				throw new CertificateException("Unkown certificate type");
			}
		} else {
			throw new CertificateException("Unkown start/end headers in certificate!");
		}
	}

	private static Certificate generateCertificate(InputStream is) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		Certificate certificate = factory.generateCertificate(is);
		return certificate;
	}

	private static BufferedInputStream extractCerticate(String pemCertString) {

		int beginHeader = pemCertString.indexOf(BEGIN_HEADER) + BEGIN_HEADER.length();
		int endHeader = pemCertString.indexOf(END_HEADER);

		StringBuffer formattedCert = new StringBuffer();
		formattedCert.append(BEGIN_HEADER);
		formattedCert.append("\n");
		formattedCert.append(pemCertString.substring(beginHeader, endHeader).replaceAll("\\s+", ""));
		formattedCert.append("\n");
		formattedCert.append(END_HEADER);

		pemCertString = formattedCert.toString();

		InputStream is = new ByteArrayInputStream(((String) pemCertString).getBytes());
		BufferedInputStream bis = new BufferedInputStream(is);
		return bis;
	}

	static boolean isPEMCertificate(Object certificate) {
		if (certificate != null && certificate instanceof String && containsCorrectPemHeaders((String) certificate)) {
			log.debug("Found possible PEM-encoded certificate in httpheader {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

	static boolean containsCorrectPemHeaders(String pemCertString) {
		int beginHeader = pemCertString.indexOf(BEGIN_HEADER);
		int endHeader = pemCertString.indexOf(END_HEADER);
		return beginHeader != -1 && endHeader != -1;
	}

	static boolean isX509Certificate(Object certificate) {
		if (certificate instanceof X509Certificate) {
			log.debug("Found X509Certificate in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

}
