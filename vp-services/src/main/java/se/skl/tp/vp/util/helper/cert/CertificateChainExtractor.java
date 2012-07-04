package se.skl.tp.vp.util.helper.cert;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

/**
 * Extractor used when extracting certificate from the certificate chain.
 * 
 */
public class CertificateChainExtractor extends CertificateExtractorBase implements CertificateExtractor {

	private static Logger log = LoggerFactory.getLogger(CertificateChainExtractor.class);

	public CertificateChainExtractor(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
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
						"VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate");
			}
		} else {
			throw new VpSemanticException("VP002 no certificates. The certificate chain was null");
		}
	}
}
