package se.skl.tp.vp.util.helper.cert;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

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

		if (!this.isCallerOnWhiteList()) {
			throw new VpSemanticException("Caller was not on the white list of accepted IP-addresses.");
		}

		log.debug("Extracting X509Certificate senderId from header");

		Object certificate = this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);

		try {
			if (isX509Certificate(certificate)) {
				return extractFromX509Certificate(certificate);
			} else if (PemConverter.isPEMCertificate(certificate)) {
				return extractFromPemFormatCertificate(certificate);
			} else if (isCertificateInPlainX500PrincipalString(certificate)) {
				return extractFromPlainX500PrincipalStringFormat(certificate);
			} else {
				log.error("Unkown certificate type found in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
				throw new VpSemanticException("VP002 Exception, unkown certificate type found in httpheader "
						+ VPUtil.REVERSE_PROXY_HEADER_NAME);
			}
		} catch (Exception e) {
			log.error("Error occured parsing certificate in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME, e);
			throw new VpSemanticException("VP002 Exception occured parsing certificate in httpheader "
					+ VPUtil.REVERSE_PROXY_HEADER_NAME);
		}

	}

	private String extractFromPlainX500PrincipalStringFormat(Object certificate) {
		certificate = replaceUnwantedCharactersInString((String) certificate);
		return extractSenderIdFromPlainX500PrincipalString((String) certificate);
	}

	private String extractFromPemFormatCertificate(Object certificate) throws CertificateException {
		X509Certificate x509Certificate = PemConverter.buildCertificate(certificate);
		return extractSenderIdFromCertificate(x509Certificate);
	}

	private String extractFromX509Certificate(Object certificate) {
		X509Certificate x509Certificate = (X509Certificate) certificate;
		return extractSenderIdFromCertificate(x509Certificate);
	}

	static String replaceUnwantedCharactersInString(String certificate) {
		return certificate.replaceAll("/", ",");
	}

	private String extractSenderIdFromPlainX500PrincipalString(String certificate) {
		return extractSenderFromPrincipal(certificate);
	}

	boolean isCertificateInPlainX500PrincipalString(Object certificate) {
		if (containsCorrectCertInformation(certificate)) {
			log.debug("Found plain string certificate information in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

	static boolean isX509Certificate(Object certificate) {
		if (certificate instanceof X509Certificate) {
			log.debug("Found X509Certificate in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

	boolean containsCorrectCertInformation(Object certificate) {
		if (certificate != null && certificate instanceof String) {
			return true;
		}
		return false;
	}

	private boolean isCallerOnWhiteList() {
		final String ip = VPUtil.extractIpAddress((String) this.getMuleMessage().getProperty(VPUtil.REMOTE_ADDR));

		log.debug("Check if caller {} is in white list..", ip);

		if (VPUtil.isWhitespace(ip)) {
			throw new VpSemanticException(
					"Could not extract the IP address of the caller. Cannot check whether caller is on the white list");
		}

		if (VPUtil.isWhitespace(this.getWhiteList())) {
			throw new VpSemanticException(
					"Could not check whether the caller is on the white list because the white list was empty.");
		}

		final String[] ips = this.getWhiteList().split(",");

		for (final String s : ips) {
			if (s.trim().equals(ip.trim())) {
				log.debug("Caller found in white list");
				return true;
			}
		}

		log.debug("Caller NOT found in white list");
		return false;
	}

}
