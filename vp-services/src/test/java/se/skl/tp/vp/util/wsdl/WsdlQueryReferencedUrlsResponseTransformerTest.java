package se.skl.tp.vp.util.wsdl;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;

import se.skl.tp.vp.util.wsdl.WsdlQueryReferencedUrlsResponseTransformer.BaseUrlModel;

public class WsdlQueryReferencedUrlsResponseTransformerTest {
	private WsdlQueryReferencedUrlsResponseTransformer transformer;
	String wsdl;
	String xsdImportedByWsdl;
	String xsdTypesImportedByXsd;
	String rewrittenWsdl;
	String rewrittenXsdImportedByWsdl;
	BaseUrlModel baseUrlModel;

	@Before
	public void setUp() throws Exception {
		transformer = new WsdlQueryReferencedUrlsResponseTransformer();
		baseUrlModel = transformer.new BaseUrlModel();
		wsdl = FileUtils
				.readFileToString(
						new File(
								"src/test/resources/test-wsdl-query/input-wsdl-query-tjanst1-1.0.wsdl"),
						"UTF-8");
		xsdImportedByWsdl = FileUtils
				.readFileToString(
						new File(
								"src/test/resources/test-wsdl-query/input-wsdl-query-tjanst1-1.0.xsd"),
						"UTF-8");
		xsdTypesImportedByXsd = FileUtils
				.readFileToString(
						new File(
								"src/test/resources/test-wsdl-query/input-wsdl-query-tjanst1-types-1.0.xsd"),
						"UTF-8");
		rewrittenWsdl = FileUtils
				.readFileToString(
						new File(
								"src/test/resources/test-wsdl-query/rewritten-wsdl-query-tjanst1-1.0.wsdl"),
						"UTF-8");
		rewrittenXsdImportedByWsdl = FileUtils
				.readFileToString(
						new File(
								"src/test/resources/test-wsdl-query/rewritten-wsdl-query-tjanst1-1.0.xsd"),
						"UTF-8");
	}

	@Test
	public void testNoUrlRewrite_if_no_url_params() throws Exception {
		assertXmlSimilar(wsdl,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(wsdl, baseUrlModel));
		assertXmlSimilar(xsdImportedByWsdl,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(xsdImportedByWsdl,
						baseUrlModel));
		assertXmlSimilar(xsdTypesImportedByXsd,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(
						xsdTypesImportedByXsd, baseUrlModel));
	}

	@Test
	public void testUrlRewrite_with_url_params() throws Exception {
		baseUrlModel.scheme = "https";
		baseUrlModel.host = "vp-loadbalancer-dns-name";
		baseUrlModel.port = "443";
		assertXmlSimilar(rewrittenWsdl,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(wsdl, baseUrlModel));
		assertXmlSimilar(rewrittenXsdImportedByWsdl,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(xsdImportedByWsdl,
						baseUrlModel));
		assertXmlSimilar(xsdTypesImportedByXsd,
				transformer.replaceBaseUrlPartsInWsdlOrXsd(
						xsdTypesImportedByXsd, baseUrlModel));
	}

	void assertXmlSimilar(String expectedXml, String actualXml)
			throws Exception {
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
		Diff diff = XMLUnit.compareXML(expectedXml, actualXml);
		assertTrue(diff.toString(), diff.similar());
	}
}
