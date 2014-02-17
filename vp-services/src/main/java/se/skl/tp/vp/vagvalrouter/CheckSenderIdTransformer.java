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
package se.skl.tp.vp.vagvalrouter;

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.cert.CertificateExtractor;
import se.skl.tp.vp.util.helper.cert.CertificateExtractorFactory;

/**
 * CheckSenderIdTransformer responsible to extract senderId to session variable.
 * 
 */
public class CheckSenderIdTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(CheckSenderIdTransformer.class);
	
	private String senderIdPropertyName;

	private String whiteList;
	
	private Pattern pattern;
	
	public void setWhiteList(final String whiteList) {
		this.whiteList = whiteList;
	}
	
	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(this.senderIdPropertyName + "=([^,]+)");
		if (logger.isInfoEnabled()) {
			logger.info("senderIdPropertyName set to: " + senderIdPropertyName);
		}
	}


    /**
     * Message aware transformer that extracts senderId to session variable
     */
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
    	
		log.debug("Exists x-vp-sender-id as inbound property {}?", VagvalRouter.X_VP_SENDER_ID);
		String senderId = message.getProperty(VagvalRouter.X_VP_SENDER_ID, PropertyScope.INBOUND, null);

		if (senderId != null) {
			log.debug("Yes, sender id extracted from inbound property {}: {}, check whitelist!", VagvalRouter.X_VP_SENDER_ID, senderId);

			/*
			 * x-vp-sender-id exist as inbound property, a mandatory check against the whitelist of
			 * ip addresses is needed. VPUtil.checkCallerOnWhiteList throws VpSemanticException in
			 * case ip address is not in whitelist.
			 */
			String callersIp = VPUtil.extractIpAddress(message);
			if(!VPUtil.isCallerOnWhiteList(callersIp, whiteList, VPUtil.SENDER_ID)){
				throw new VpSemanticException("Caller was not on the white list of accepted IP-addresses. IP-address: " 
						+ callersIp + ". HTTP header that caused checking: " + VPUtil.SENDER_ID);
			}

		} else {
			/*
			 * x-vp-sender-id was not found in inbound properties, lets look up sender id into the certificate instead.
			 * 
			 * Two flavours exist when looking for certificate information:
			 * Certificate can be provided in http header x-vp-auth-cert, e.g when using a reverse proxy.
			 * Certificate can be provided using SSL/TLS.
			 */
			try {
				log.debug("No, look into the senders certificate instead");
				CertificateExtractorFactory certificateExtractorFactory = new CertificateExtractorFactory(message, pattern, whiteList);
				CertificateExtractor certHelper = certificateExtractorFactory.creaetCertificateExtractor();
				senderId = certHelper.extractSenderIdFromCertificate();
				log.debug("Sender id extracted from certificate {}", senderId);
				
			} catch (final VpSemanticException e) {
				log.warn("Could not extract sender id from certificate. Reason: {} ", e.getMessage());
			} 	
		}
		
		// Make sure the sender id is set in session scoped property for e.g authorization and logging
		if (senderId != null) {
			message.setProperty(VPUtil.SENDER_ID, senderId, PropertyScope.SESSION);
		}
          
        return message;
    }

}
