package se.skl.tp.vp.util.helper.cert;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

/**
 * 
 * Extract certificate information from the http header.
 */
public class CertificateHeaderExtractor extends CertificateExtractorBase implements CertificateExtractor {

	private static final String BEGIN_HEADER = "-----BEGIN CERTIFICATE-----";
	private static final String END_HEADER = "-----END CERTIFICATE-----";

	private static Logger log = LoggerFactory.getLogger(CertificateHeaderExtractor.class);

	public CertificateHeaderExtractor(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	@Override
	public X509Certificate extractCertificate() throws VpSemanticException {
		log.debug("Extracting X509Certificate from header");
		return this.extractCertFromHeader();
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
			} else if (isPEMCertificate(certificate)) {
				return buildCertificateFromPem(certificate);
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

	/*
	 * PEM - (Privacy Enhanced Mail) Base64 encoded DER certificate, enclosed
	 * between "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----"
	 */
	static X509Certificate buildCertificateFromPem(Object pemCert) throws CertificateException {

		if (log.isDebugEnabled()) {
			log.debug("Got possible PEM-encoded certificate");
			log.debug((String) pemCert);
		}

		String pemCertString = (String) pemCert;

		if (containsCorrectHeaders(pemCertString)) {

			InputStream certificateInfo = extractCerticate(pemCertString);
			Certificate certificate = generateCertificate(certificateInfo);

			if (certificate instanceof X509Certificate) {
				log.debug("Certificate converted to X509Certificate!");
				log.debug("Certificate principalname: "
						+ ((X509Certificate) certificate).getSubjectX500Principal().getName());
				return (X509Certificate) certificate;
			} else {
				throw new CertificateException("Unkown certificate type");
			}
		} else {
			throw new CertificateException("Unkown start/end headers in certificate!");
		}
	}

	private static Certificate generateCertificate(InputStream is) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		Certificate certificate = factory.generateCertificate(is);
		return certificate;
	}

	private static BufferedInputStream extractCerticate(String pemCertString) {

		int beginHeader = pemCertString.indexOf(BEGIN_HEADER);
		int endHeader = pemCertString.indexOf(END_HEADER);

		StringBuffer formattedCert = new StringBuffer();
		formattedCert.append(pemCertString.substring(beginHeader, endHeader));
		formattedCert.append(END_HEADER);
		pemCertString = formattedCert.toString();

		InputStream is = new ByteArrayInputStream(((String) pemCertString).getBytes());
		BufferedInputStream bis = new BufferedInputStream(is);
		return bis;
	}

	private static boolean containsCorrectHeaders(String pemCertString) {
		int beginHeader = pemCertString.indexOf(BEGIN_HEADER);
		int endHeader = pemCertString.indexOf(END_HEADER);
		return beginHeader != -1 && endHeader != -1;
	}

	private boolean isPEMCertificate(Object certificate) {
		if (certificate != null && certificate instanceof String) {
			log.debug("Found possible PEM-encoded certificate in httpheader {}", VPUtil.REVERSE_PROXY_HEADER_NAME);
			return true;
		}
		return false;
	}

	private boolean isX509Certificate(Object certificate) {
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
