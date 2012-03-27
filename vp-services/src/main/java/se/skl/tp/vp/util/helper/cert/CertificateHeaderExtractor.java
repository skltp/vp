package se.skl.tp.vp.util.helper.cert;

import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

/**
 * 
 * Extract certificate information from the http header
 * VPUtil.REVERSE_PROXY_HEADER_NAME.
 */
public class CertificateHeaderExtractor extends CertificateExtractorBase implements CertificateExtractor {

	private static Logger log = LoggerFactory.getLogger(CertificateHeaderExtractor.class);

	public CertificateHeaderExtractor(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	@Override
	public String extractSenderIdFromCertificate() {
		log.debug("Extracting X509Certificate senderId from header");
		X509Certificate certificate = extractCertFromHeader();
		return extractSenderIdFromCertificate(certificate);
	}

	/**
	 * Extract the certificate from the http header
	 * 
	 * @return
	 */
	private X509Certificate extractCertFromHeader() {
		log.debug("Extracting from http header...");

		if (!this.isCallerOnWhiteList()) {
			throw new VpSemanticException("Caller was not on the white list of accepted IP-addresses.");
		}

		Object certificate = this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);

		try {
			if (isX509Certificate(certificate)) {
				return (X509Certificate) certificate;
			} else if (PemConverter.isPEMCertificate(certificate)) {
				return PemConverter.buildCertificate(certificate);
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

	static boolean isX509Certificate(Object certificate) {
		if (certificate instanceof X509Certificate) {
			log.debug("Found X509Certificate in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

	private boolean isCallerOnWhiteList() {
		final String ip = VPUtil.extractIpAddress((String) this.getMuleMessage().getProperty(VPUtil.REMOTE_ADDR));

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
				return true;
			}
		}

		return false;
	}

}
