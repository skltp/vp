package se.skl.tp.vp.util;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;

public final class VPUtil {

	private static Logger log = LoggerFactory.getLogger(VPUtil.class);
	
	public static String getSenderIdFromCertificate(MuleMessage message, final Pattern pattern) {

		String senderId = null;
		Certificate[] peerCertificateChain = (Certificate[]) message
				.getProperty("PEER_CERTIFICATES");

		if (peerCertificateChain != null) {
			// Check type of first certificate in the chain, this should be the
			// clients certificate
			if (peerCertificateChain[0] instanceof X509Certificate) {
				X509Certificate cert = (X509Certificate) peerCertificateChain[0];
				String principalName = cert.getSubjectX500Principal().getName();
				Matcher matcher = pattern.matcher(principalName);
				if (matcher.find()) {
					senderId = matcher.group(1);
				} else {
					String errorMessage = ("VP002 No senderId found in Certificate: " + principalName);
					log.info(errorMessage);
					throw new VpSemanticException(errorMessage);

				}
			} else {
				String errorMessage = ("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate: " + peerCertificateChain[0]);
				log.info(errorMessage);
				throw new VpSemanticException(errorMessage);
			}
		} else {
			String errorMessage = ("VP002 No senderId found in Certificate: No certificate chain found from client");
			log.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		
		// Check if this is coded in hex (HCC Funktionscertifikat does that!)
		if (senderId.startsWith("#")) {
			return convertFromHexToString(senderId.substring(5));
		} else {
			return senderId;			
		}
	}
	
	private static String convertFromHexToString(final String hexString) {
		byte [] txtInByte = new byte [hexString.length() / 2];
		int j = 0;
		for (int i = 0; i < hexString.length(); i += 2)
		{
			txtInByte[j++] = Byte.parseByte(hexString.substring(i, i + 2), 16);
		}
		return new String(txtInByte);
	}
}
