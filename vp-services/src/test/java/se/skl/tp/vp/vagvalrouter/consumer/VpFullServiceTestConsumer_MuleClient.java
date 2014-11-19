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
package se.skl.tp.vp.vagvalrouter.consumer;

import static org.soitoolkit.commons.xml.XPathUtil.appendXmlFragment;
import static org.soitoolkit.commons.xml.XPathUtil.getXml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.module.xml.stax.MapNamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;
import org.soitoolkit.commons.xml.XPathUtil;
import org.w3.wsaddressing10.AttributedURIType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import se.skl.tp.vp.soitoolkit060.duplicate.RestClient;
import se.skltp.domain.subdomain.getproducdetail.v1.GetProductDetailResponse;
import se.skltp.domain.subdomain.getproducdetail.v1.GetProductDetailType;
import se.skltp.domain.subdomain.getproducdetail.v1.ObjectFactory;
import se.skltp.domain.subdomain.getproducdetail.v1.Product;

/**
 * Test consumer based on MuleClient and plain https, using jaxb, jaxp and xpath to create and parse soap-envelopes for requests and repsonses.
 * Works only with a MuleContext, e.g. as part of a Mule Integration test.
 * In the constructor the https-connector to use is specified.
 * 
 * @author magnuslarsson
 *
 */

public class VpFullServiceTestConsumer_MuleClient {
	
	private static final Logger log = LoggerFactory.getLogger(VpFullServiceTestConsumer_MuleClient.class);

	private static final JaxbUtil jaxbUtil = new JaxbUtil("org.w3.wsaddressing10:se.skltp.domain.subdomain.getproducdetail.v1");

	private static final ObjectFactory OF = new ObjectFactory();
	private static final org.w3.wsaddressing10.ObjectFactory OF_ADDR = new org.w3.wsaddressing10.ObjectFactory();
	
	private RestClient restClient = null;

	private static final Map<String, String> namespaceMap = new HashMap<String, String>();
	
	static {
		namespaceMap.put("soap",    "http://schemas.xmlsoap.org/soap/envelope/");
		namespaceMap.put("it-int",  "urn:riv:itintegration:registry:1");
		namespaceMap.put("interop", "urn:riv:interoperability:headers:1");
		namespaceMap.put("service", "urn:riv:domain:subdomain:GetProductDetail:1:rivtabp20");
		namespaceMap.put("serviceContract", "urn:riv:domain:subdomain:GetProductDetailResponder:1");
	}

	private static final String responseTemplate =
		"<soapenv:Envelope " + 
		  "xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' " +  
		  "xmlns:urn='urn:riv:interoperability:headers:1' " + 
		  "xmlns:urn1='urn:riv:itintegration:registry:1' >" + 
		  "<soapenv:Header>" + 
		  "</soapenv:Header>" +
		  "<soapenv:Body>" +
		  "</soapenv:Body>" +
		"</soapenv:Envelope>";
	
	
	public VpFullServiceTestConsumer_MuleClient(MuleContext muleContext, String httpsConnector, int timeout_ms) {
		restClient = new RestClient(muleContext , httpsConnector, timeout_ms);
	}

	public VpFullServiceTestConsumer_MuleClient(MuleContext muleContext, String httpsConnector) {
		restClient = new RestClient(muleContext , httpsConnector);
	}

	public Product callGetProductDetail(String productId, String serviceAddress, String logicalAddress) throws Exception {
		return callGetProductDetail(productId, serviceAddress, logicalAddress, null);
	}

	public Product callGetProductDetail(String productId, String serviceAddress, String logicalAddress, Map<String, String> properties) throws Exception {
		GetProductDetailType request = new GetProductDetailType();
		request.setProductId(productId);

		AttributedURIType logicalAddressHeader = new AttributedURIType();
		logicalAddressHeader.setValue(logicalAddress);

		String xmlRequest = marshall(logicalAddressHeader, request);
		
        MuleMessage muleResponse = restClient.doHttpPostRequest_XmlContent(serviceAddress, xmlRequest, properties);

        InputStream xmlResponse = (InputStream)muleResponse.getPayload();
        Document domResponse = createDocument(xmlResponse);
        
        NodeList fault = XPathUtil.getXPathResult(domResponse, namespaceMap, "/soap:Envelope/soap:Body/soap:Fault");
        if (fault.getLength() > 0) {
        	throw new RuntimeException(XPathUtil.getXml(domResponse));
        }
        
        GetProductDetailResponse response = unmarshall(domResponse);

		Product p = response.getProduct();
		return p;
	}

	
	private Document createDocument(InputStream content) {
		try {
			return getBuilder().parse(content);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String marshall(AttributedURIType logicalAddressHeader, GetProductDetailType request) {

        String xmlHeader = jaxbUtil.marshal(OF_ADDR.createTo(logicalAddressHeader));
		String xmlBody = jaxbUtil.marshal(OF.createGetProductDetailElem(request));

        log.debug("header:\n{}", xmlHeader);
        log.debug("body:\n{}", xmlBody);
        
		XPath xpath = XPathFactory.newInstance().newXPath();
	    xpath.setNamespaceContext(new MapNamespaceContext(namespaceMap));

		Object result;
		Document respDoc = createDocument(responseTemplate, "UTF-8");
		try {
	    	
			XPathExpression xpathHeader = xpath.compile("/soap:Envelope/soap:Header");
			result = xpathHeader.evaluate(respDoc, XPathConstants.NODESET);
			
			NodeList list = (NodeList)result; 
			log.debug("header xpath result: {}", list.getLength());
			Node nodeHeader = list.item(0);
	        
	    	appendXmlFragment(nodeHeader, xmlHeader);

	    	
			
			XPathExpression xpathBody = xpath.compile("/soap:Envelope/soap:Body");
			result = xpathBody.evaluate(respDoc, XPathConstants.NODESET);
			
			list = (NodeList)result; 
			log.debug("body xpath result: {}", list.getLength());
			Node nodeBody = list.item(0);
	        
	    	appendXmlFragment(nodeBody, xmlBody);
			    	
	    	
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		String xml = getXml(respDoc);
		return xml;
	}

	private GetProductDetailResponse unmarshall(Document doc) {
		Object result;
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
		    xpath.setNamespaceContext(new MapNamespaceContext(namespaceMap));

			XPathExpression xpathRequest = xpath.compile("/soap:Envelope/soap:Body/serviceContract:getProductDetailResponse");

			result = xpathRequest.evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		NodeList list = (NodeList)result; 
		log.debug("getProductDetailResponse xpath result: {}", list.getLength());
		Node node = list.item(0);

		// Lookup the fragment...
		GetProductDetailResponse resp = (GetProductDetailResponse)jaxbUtil.unmarshal(node);
		log.debug("getProductDetailResponse jaxb unmarshal result: {}", resp.getProduct().getId());

		return resp;
	}
	
	private Document createDocument(String content, String charset) {
		try {
			InputStream is = new ByteArrayInputStream(content.getBytes(charset));
			return createDocument(is);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private DocumentBuilder getBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		return builder;
	}
}