package se.skl.tp.vp.util.helper.cert;

import java.security.cert.X509Certificate;

import se.skl.tp.vp.exceptions.VpSemanticException;

/**
 * Responsible for dealing with extracting certificates
 * 
 */
public interface CertificateExtractor {

	/**
	 * Extract a X509Certificate from the mule message.
	 * 
	 * @return a X509Certificate
	 * @throws VpSemanticException
	 *             in case of any errors during extract
	 */
	public X509Certificate extractCertificate() throws VpSemanticException;

	/**
	 * Extract the sender id from a X509 certificate
	 * 
	 * @param certificate
	 * @return
	 */
	public String extractSenderIdFromCertificate(final X509Certificate certificate);

}
