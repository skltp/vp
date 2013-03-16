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

		// Check whitelist (throws an exception if not ok)
		VPUtil.checkCallerOnWhiteList(getMuleMessage(), getWhiteList(), VPUtil.REVERSE_PROXY_HEADER_NAME);

		log.debug("Extracting X509Certificate senderId from header");

		Object certificate = this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND);

		try {
			if (isX509Certificate(certificate)) {
				return extractFromX509Certificate(certificate);
			} else if (PemConverter.isPEMCertificate(certificate)) {
				return extractFromPemFormatCertificate(certificate);
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
			log.debug("Found X509Certificate in httpheader: {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}
}
