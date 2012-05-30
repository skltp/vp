package se.skl.tp.vp.util.helper.cert;


/**
 * Responsible for dealing with extracting certificates
 * 
 */
public interface CertificateExtractor {

	/**
	 * Extract the sender id from a X509 certificate
	 * 
	 * @param certificate
	 * @return
	 */
	public String extractSenderIdFromCertificate();

}
