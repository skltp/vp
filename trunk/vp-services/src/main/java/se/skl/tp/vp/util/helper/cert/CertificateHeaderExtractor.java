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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.vagvalrouter.VagvalRouter;

/**
 * Extractor used when extracting certificate from header
 * VPUtil.REVERSE_PROXY_HEADER_NAME. This is used when running in reverse proxy
 * mode.
 * 
 */
public class CertificateHeaderExtractor extends CertificateExtractorBase implements CertificateExtractor {

	private static Logger log = LoggerFactory.getLogger(CertificateHeaderExtractor.class);

	public CertificateHeaderExtractor(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	@Override
	public String extractSenderIdFromCertificate() {

		log.debug("Extracting from http header...");

		// Check whitelist
		String callersIp = VPUtil.extractIpAddress(getMuleMessage());
		if(!VPUtil.isCallerOnWhiteList(callersIp, getWhiteList(), VagvalRouter.REVERSE_PROXY_HEADER_NAME)){
			throw VPUtil.createVP011Exception(callersIp, VagvalRouter.REVERSE_PROXY_HEADER_NAME);
		}

		log.debug("Extracting X509Certificate senderId from header");

		Object certificate = this.getMuleMessage().getProperty(VagvalRouter.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);

		try {
			if (isX509Certificate(certificate)) {
				return extractFromX509Certificate(certificate);
			} else if (PemConverter.isPEMCertificate(certificate)) {
				return extractFromPemFormatCertificate(certificate);
			} else {
				log.error("Unkown certificate type found in httpheader: {}", VagvalRouter.REVERSE_PROXY_HEADER_NAME);
				throw new VpSemanticException("VP002 Exception, unkown certificate type found in httpheader "
						+ VagvalRouter.REVERSE_PROXY_HEADER_NAME);
			}
		} catch (Exception e) {
			log.error("Error occured parsing certificate in httpheader: {}", VagvalRouter.REVERSE_PROXY_HEADER_NAME, e);
			throw new VpSemanticException("VP002 Exception occured parsing certificate in httpheader "
					+ VagvalRouter.REVERSE_PROXY_HEADER_NAME);
		}

	}

	private String extractFromPemFormatCertificate(Object certificate) throws CertificateException {
		X509Certificate x509Certificate = PemConverter.buildCertificate(certificate);
		return extractSenderIdFromCertificate(x509Certificate);
	}

	private String extractFromX509Certificate(Object certificate) {
		X509Certificate x509Certificate = (X509Certificate) certificate;
		return extractSenderIdFromCertificate(x509Certificate);
	}

	static boolean isX509Certificate(Object certificate) {
		if (certificate instanceof X509Certificate) {
			log.debug("Found X509Certificate in httpheader: {}", VagvalRouter.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}
}
