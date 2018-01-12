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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.WhiteListHandler;

/**
 * Extractor used when extracting certificate from the certificate chain.
 * 
 */
public class CertificateChainExtractor extends CertificateExtractorBase implements CertificateExtractor {

	private static Logger log = LoggerFactory.getLogger(CertificateChainExtractor.class);

	public CertificateChainExtractor(MuleMessage muleMessage, Pattern pattern, WhiteListHandler whiteListHandler) {
		super(muleMessage, pattern, whiteListHandler);
	}

	@Override
	public String extractSenderIdFromCertificate() {
		log.debug("Extracting X509Certificate senderId from chain");
		X509Certificate certificate = extraxtCertFromChain();
		return extractSenderIdFromCertificate(certificate);
	}

	/**
	 * Extract the certificate from the cert chain
	 * 
	 * @return
	 */
	private X509Certificate extraxtCertFromChain() {
		final Certificate[] certificateChain = (Certificate[]) this.getMuleMessage().getProperty(
				VPUtil.PEER_CERTIFICATES, PropertyScope.OUTBOUND);
		if (certificateChain != null) {
			try {
				return (X509Certificate) certificateChain[0];
			} catch (final ClassCastException e) {
				throw new VpSemanticException(
						VpSemanticErrorCodeEnum.VP002 + " No senderId found in Certificate: First certificate in chain is not X509Certificate",
						VpSemanticErrorCodeEnum.VP002);
			}
		} else {
			throw new VpSemanticException(VpSemanticErrorCodeEnum.VP002 + " no certificates. The certificate chain was null", VpSemanticErrorCodeEnum.VP002);
		}
	}
}
