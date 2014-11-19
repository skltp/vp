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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

public class WsdlQueryIntegrationTest extends AbstractTestCase {
	private static final String SERVICE_URL = "http://localhost:8080/vp/tjanst1";
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config","vp-config-override");
	private HttpClient httpClient;
	
	public WsdlQueryIntegrationTest() {
		super();
		
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);		
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"teststub-services/vp-virtuell-tjanst-teststub-service.xml";
	}
		
	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();
		
		httpClient = new HttpClient();
	}
	
	@Test
	public void testWsdlLookupWithoutLoadBalancerForwardedInfo() throws Exception {
		String wsdl = doHttpGet(SERVICE_URL + "?wsdl", null);

		// can't use XMLUnit here - CXF changes the order or wsdl:message elemenst making XMLUnit-diff fail
		assertTrue("Make sure we got a WSDL doc: " + wsdl, wsdl.trim().endsWith("</wsdl:definitions>"));
		String xsdUrl = SERVICE_URL + "?xsd=GetProductDetailResponder_1.0.xsd";
		assertTrue("CXF should do default url rewrite: " + wsdl, wsdl.contains("schemaLocation=\"" + xsdUrl + "\""));
		assertTrue("CXF should do default url rewrite: " + wsdl, wsdl.contains("<soap:address location=\"" + SERVICE_URL + "\""));
		
		String xsd = doHttpGet(xsdUrl, null);
		assertTrue("Make sure we got the XSD doc: " + xsd, xsd.trim().endsWith("</xs:schema>"));
	}	

	@Test
	public void testWsdlLookupWithLoadBalancerForwardedInfo() throws Exception {
		Map<String, String> httpHeaders = new HashMap<String, String>();
		httpHeaders.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_PROTO"), "http");
		httpHeaders.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_HOST"), "vp-loadbalancer-dns-name");
		httpHeaders.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_PORT"), "443");
		String wsdl = doHttpGet(SERVICE_URL + "?wsdl", httpHeaders);
		
		String rewrittenServiceUrl = "http://vp-loadbalancer-dns-name:443/vp/tjanst1";
		// can't use XMLUnit here - CXF changes the order or wsdl:message elements making XMLUnit-diff fail
		String xsdUrl = rewrittenServiceUrl + "?xsd=GetProductDetailResponder_1.0.xsd";
		assertTrue("CXF should do default url rewrite: " + wsdl, wsdl.contains("schemaLocation=\"" + xsdUrl + "\""));
		assertTrue("CXF should do default url rewrite: " + wsdl, wsdl.contains("<soap:address location=\"" + rewrittenServiceUrl + "\""));		
	}	

	private String doHttpGet(String url, Map<String, String> httpHeaders) throws Exception {
		GetMethod get = new GetMethod(url);
		if (httpHeaders != null) {
			for (String headerName : httpHeaders.keySet()) {
				get.addRequestHeader(headerName, httpHeaders.get(headerName));
			}
		}
		int retStatus = httpClient.executeMethod(get);
		assertEquals(HttpStatus.SC_OK, retStatus);
		String response = new String(get.getResponseBody(), "UTF-8");
		return response;
	}
	
}