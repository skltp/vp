/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
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
