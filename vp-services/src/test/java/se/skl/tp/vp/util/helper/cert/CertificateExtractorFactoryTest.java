package se.skl.tp.vp.util.helper.cert;

import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vp.util.VPUtil;

public class CertificateExtractorFactoryTest {

	@Test
	public void extractFromHeaderWhenReveresedProxyHeaderExist() throws Exception {

		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		Mockito.when(msg.getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME, PropertyScope.INVOCATION)).thenReturn("ANY VALUE");
		Pattern pattern = null;

		CertificateExtractorFactory factory = new CertificateExtractorFactory(msg, pattern, "127.0.0.1");
		CertificateExtractor certificateExtractor = factory.creaetCertificateExtractor();

		assertTrue(certificateExtractor instanceof CertificateHeaderExtractor);
	}

	@Test
	public void extractFromChainIsDefault() throws Exception {

		final DefaultMuleMessage msg = Mockito.mock(DefaultMuleMessage.class);
		Pattern pattern = null;

		CertificateExtractorFactory factory = new CertificateExtractorFactory(msg, pattern, "127.0.0.1");
		CertificateExtractor certificateExtractor = factory.creaetCertificateExtractor();

		assertTrue(certificateExtractor instanceof CertificateChainExtractor);
	}

}
