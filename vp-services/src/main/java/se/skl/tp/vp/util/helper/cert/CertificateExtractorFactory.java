package se.skl.tp.vp.util.helper.cert;

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.VPHelperSupport;

/**
 * Create a CertificateExtractor based on the mule message.
 */
public class CertificateExtractorFactory extends VPHelperSupport {

	private static Logger log = LoggerFactory.getLogger(CertificateExtractorFactory.class);

	public CertificateExtractorFactory(MuleMessage muleMessage, Pattern pattern, String whiteList) {
		super(muleMessage, pattern, whiteList);
	}

	/**
	 * Extract a X509Certificate
	 * 
	 * @param reverseProxyMode
	 * @return
	 */
	public CertificateExtractor creaetCertificateExtractor() {

		final boolean isReverseProxy = this.isReverseProxy();
		log.debug("Get extractor for X509Certificate. Reverse proxy mode: {}", isReverseProxy);

		if (isReverseProxy) {
			return new CertificateHeaderExtractor(getMuleMessage(), getPattern(), getWhiteList());
		} else {
			return new CertificateChainExtractor(getMuleMessage(), getPattern(), getWhiteList());
		}
	}

	private boolean isReverseProxy() {
		return this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME, PropertyScope.INVOCATION) != null;
	}

}
