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

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.VPHelperSupport;

/**
 * Create a CertificateExtractor based on the mule message.
 */
public class CertificateExtractorFactory extends VPHelperSupport {

	private static Logger log = LoggerFactory.getLogger(CertificateExtractorFactory.class);

	public CertificateExtractorFactory(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	/**
	 * Extract a X509Certificate
	 * 
	 * @param reverseProxyMode
	 * @return
	 */
	public CertificateExtractor creaetCertificateExtractor() {

		final boolean isReverseProxy = this.isReverseProxy();
		log.debug("Get extractor for X509Certificate. Reverse proxy mode: {}", isReverseProxy);

		if (isReverseProxy) {
			return new CertificateHeaderExtractor(getMuleMessage(), getPattern(), getWhiteList());
		} else {
			return new CertificateChainExtractor(getMuleMessage(), getPattern(), getWhiteList());
		}
	}

	private boolean isReverseProxy() {
		return this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND) != null;
	}

}
