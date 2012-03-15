package se.skl.tp.vp.util.helper.cert;

import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.helper.VPHelperSupport;

public class CertificateExtractorBase extends VPHelperSupport {

	private static Logger log = LoggerFactory.getLogger(CertificateExtractorBase.class);

	public CertificateExtractorBase(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	public String extractSenderIdFromCertificate(final X509Certificate certificate) {

		log.debug("Extracting sender id from certificate.");

		if (certificate == null) {
			throw new IllegalArgumentException("Cannot extract any sender because the certificate was null");
		}

		if (this.getPattern() == null) {
			throw new IllegalArgumentException("Cannot extract any sender becuase the pattern used to find it was null");
		}

		final String principalName = certificate.getSubjectX500Principal().getName();
		final Matcher matcher = this.getPattern().matcher(principalName);

		if (matcher.find()) {
			final String senderId = matcher.group(1);

			log.debug("Found sender id: {}", senderId);
			return senderId.startsWith("#") ? this.convertFromHexToString(senderId.substring(5)) : senderId;
		} else {
			throw new VpSemanticException("VP002 No senderId found in Certificate: " + principalName);
		}
	}

	private String convertFromHexToString(final String hexString) {
		byte[] txtInByte = new byte[hexString.length() / 2];
		int j = 0;
		for (int i = 0; i < hexString.length(); i += 2) {
			txtInByte[j++] = Byte.parseByte(hexString.substring(i, i + 2), 16);
		}
		return new String(txtInByte);
	}

}
