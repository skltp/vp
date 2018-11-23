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

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.http.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Change URL-parts in WSDL's and XSD's returned from a
 * "GET https://reverse-proxy:port/myService?wsdl" reguest so URL's contained in
 * WSDL's and XSD's points to the reverse-proxy/loadbalancer, instead of
 * pointing directly to a server behind a reverse proxy (which is unreachable
 * for a client).
 * <p>
 * The WSDL/XSD contained URL's needs to be changed for subsequent client
 * requests to hit the reverse-proxy.
 * <p>
 * Ref: issue https://skl-tp.atlassian.net/browse/NTP-57
 * <p>
 * Curl example for testing:
 * 
 * <pre>
 * curl -H "X-Forwarded-Proto: https" -H "x-forwarded-host: loadbalancer" -H "x-forwarded-port: 443" http://localhost:8080/vp/tjanst1?wsdl
 * </pre>
 */
public class WsdlQueryReferencedUrlsResponseTransformer extends
		AbstractMessageTransformer {
	private static final Logger log = LoggerFactory
			.getLogger(WsdlQueryReferencedUrlsResponseTransformer.class);
	private static final String HTTP_REQUEST_MESSAGE_PROPERTY = HttpConnector.HTTP_REQUEST_PROPERTY;
	private static final String HTTP_METHOD_MESSAGE_PROPERTY = HttpConnector.HTTP_METHOD_PROPERTY;
	private static final String HTTP_METHOD_GET = "GET";
	/**
	 * Default names for HTTP headers set by reverse-proxy.
	 * <p>
	 * Ref: http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#x-headers
	 * <p>
	 * Ref: https://devcenter.heroku.com/articles/http-routing
	 * <p>
	 * Note: a reverse-proxy/loadbalancer only needs to set headers for the URL
	 * parts that it changes.
	 */
	// proto/scheme: https/http
	private String httpHeaderNameForwardedProto = "X-Forwarded-Proto";
	private String httpHeaderNameForwardedHost = "X-Forwarded-Host";
	private String httpHeaderNameForwardedPort = "X-Forwarded-Port";

	public void setHttpHeaderNameForwardedProto(
			String httpHeaderNameForwardedProto) {
		this.httpHeaderNameForwardedProto = httpHeaderNameForwardedProto;
		log.debug("Set name for httpHeaderNameForwardedProto: {}",
				httpHeaderNameForwardedProto);
	}

	public void setHttpHeaderNameForwardedHost(
			String httpHeaderNameForwardedHost) {
		this.httpHeaderNameForwardedHost = httpHeaderNameForwardedHost;
		log.debug("Set name for httpHeaderNameForwardedHost: {}",
				httpHeaderNameForwardedHost);
	}

	public void setHttpHeaderNameForwardedPort(
			String httpHeaderNameForwardedPort) {
		this.httpHeaderNameForwardedPort = httpHeaderNameForwardedPort;
		log.debug("Set name for httpHeaderNameForwardedPort: {}",
				httpHeaderNameForwardedPort);
	}

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {

		String httpMethod = message
				.getInboundProperty(HTTP_METHOD_MESSAGE_PROPERTY);
		// it's always GET for quering with ?wsdl
		if (!HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)) {
			return message;
		}

		String httpRequest = message
				.getInboundProperty(HTTP_REQUEST_MESSAGE_PROPERTY);
		String request = httpRequest.trim().toLowerCase();
		if (request.endsWith("?wsdl") || request.endsWith(".xsd")) {
			// request for WSDL or XSD
			log.debug("Detected request for WSDL/XSD: {}", request);

			BaseUrlModel baseUrlForwardedParts = extractForwardedHttpHeadersForBaseUrl(message);

			if (baseUrlForwardedParts.isAnyUrlPartSet()) {
				replaceBaseUrlPartsInWsdlOrXsd(message, baseUrlForwardedParts);
			}
		}

		return message;
	}

	protected void replaceBaseUrlPartsInWsdlOrXsd(MuleMessage message,
			BaseUrlModel baseUrlForwardedParts) {
		Object objPayload = message.getPayload();

		if (!(objPayload instanceof String)) {
			throw new IllegalArgumentException("Unexpected payload type: "
					+ objPayload.getClass().getSimpleName());
		}

		String wsdlOrXsd = (String) objPayload;
		try {
			message.setPayload(replaceBaseUrlPartsInWsdlOrXsd(wsdlOrXsd,
					baseUrlForwardedParts));
		} catch (Exception e) {
			throw new RuntimeException(
					"Error during replace of URLs using baseUrl: "
							+ baseUrlForwardedParts.toString() + " in WSDL: "
							+ wsdlOrXsd, e);
		}
	}

	protected BaseUrlModel extractForwardedHttpHeadersForBaseUrl(
			MuleMessage message) {
		BaseUrlModel baseUrl = new BaseUrlModel();
		baseUrl.scheme = message
				.getInboundProperty(httpHeaderNameForwardedProto);
		baseUrl.host = message.getInboundProperty(httpHeaderNameForwardedHost);
		baseUrl.port = message.getInboundProperty(httpHeaderNameForwardedPort);

		log.debug("Found forwarded HTTP headers for URL parts: {}",
				baseUrl.toString());

		return baseUrl;
	}

	/**
	 * Replace inlined URLs (local URLs, not the public URLs exposed by a
	 * fronting reverse-proxy) references like: http://127.0.0.1:8080/vp/tjanst1
	 * with URLs like: ${reverseProxyBaseUrl}/vp/tjanst1.
	 * 
	 * @param wsdlOrXsd
	 * @param reverseProxyBaseUrl
	 *            The base url consists of scheme, host, port. Example:
	 *            https://loadbalancer-host:443
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	protected String replaceBaseUrlPartsInWsdlOrXsd(String wsdlOrXsd,
			BaseUrlModel reverseProxyBaseUrl) throws DocumentException,
			IOException {

		Document document = convertStringToDocument(wsdlOrXsd);

		// Dom4j with Xpath and namespaces:
		// http://whileonefork.blogspot.se/2011/01/how-to-use-dom4j-xpath-with-xml.html

		// xpath namespaces
		Map<String, String> namespaceUris = new HashMap<>();
		namespaceUris.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
		namespaceUris.put("xsd", "http://www.w3.org/2001/XMLSchema");
		namespaceUris.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");

		// <soap:address location="http://127.0.0.1:8081/vp/tjanst1"/>
		XPath xpathWsdlSoapAddressLocation = DocumentHelper
				.createXPath("/wsdl:definitions/wsdl:service/wsdl:port/soap:address/@location");
		xpathWsdlSoapAddressLocation.setNamespaceURIs(namespaceUris);
		// <xsd:import namespace="urn:skl:tjanst1:rivtabp20"
		// schemaLocation="http://127.0.0.1:8081/vp/tjanst1?xsd=tjanst1-1.0.xsd"/>
		// Note: schema imports can be present in both WSDL and XSDs, use XPath
		// that handles both cases
		XPath xpathXsdImportSchemaLocation = DocumentHelper
				.createXPath("//xsd:import/@schemaLocation");
		xpathXsdImportSchemaLocation.setNamespaceURIs(namespaceUris);

		replaceBaseUrlParts(document, xpathWsdlSoapAddressLocation,
				reverseProxyBaseUrl);
		replaceBaseUrlParts(document, xpathXsdImportSchemaLocation,
				reverseProxyBaseUrl);

		return convertDocumentToString(document);
	}

	protected void replaceBaseUrlParts(Document document, XPath xpath,
			BaseUrlModel baseUrlParts) {
		List attribList = xpath.selectNodes(document);
		for (Object attribObj : attribList) {
			if (!(attribObj instanceof Attribute)) {
				throw new RuntimeException(
						"Unexpected type returned from xpath expression: "
								+ attribObj.getClass().getSimpleName());
			}
			Attribute attribute = (Attribute) attribObj;
			String originalUrl = attribute.getText();
			String replacementUrl = replaceBaseUrlParts(originalUrl,
					baseUrlParts);
			attribute.setText(replacementUrl);
			log.debug("replaced original url: {} with url: {}");
		}
	}

	/**
	 * @param url
	 *            the original full URL
	 * @param baseUrl
	 *            the replacement baseUrl
	 * @return the full URL with baseUrl replaced
	 */
	protected String replaceBaseUrlParts(String url, BaseUrlModel baseUrl) {
		try {
			URL urlOrig = new URL(url);
			// use URI to correctly decode/encode URLs, ref:
			// http://docs.oracle.com/javase/7/docs/api/java/net/URL.html
			URI uriOrig = urlOrig.toURI();

			String scheme = baseUrl.scheme != null ? baseUrl.scheme : uriOrig
					.getScheme();
			String host = baseUrl.host != null ? baseUrl.host : uriOrig
					.getHost();
			int port = baseUrl.port != null ? Integer.valueOf(baseUrl.port)
					: uriOrig.getPort();

			URI uriNew = new URI(scheme, null, host, port, uriOrig.getPath(),
					uriOrig.getQuery(), uriOrig.getFragment());
			return uriNew.toURL().toExternalForm();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error transforming url", e);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error transforming url", e);
		}
	}

	protected Document convertStringToDocument(String xml)
			throws DocumentException {
		return DocumentHelper.parseText(xml);
	}

	protected String convertDocumentToString(Document document)
			throws IOException {
		StringWriter result = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(result, format);
		writer.write(document);
		writer.close();
		return result.toString();
	}

	/**
	 * Represent the base part of an URL.
	 */
	class BaseUrlModel {
		String scheme;
		String host;
		String port;

		public BaseUrlModel() {
		}

		public BaseUrlModel(String scheme, String host, String port) {
			this.scheme = scheme;
			this.host = host;
			this.port = port;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.getClass().getSimpleName());
			sb.append(": scheme: ");
			sb.append(scheme);
			sb.append(", host: ");
			sb.append(host);
			sb.append(", port: ");
			sb.append(port);
			return sb.toString();
		}

		public boolean isAnyUrlPartSet() {
			return scheme != null || host != null || port != null;
		}
	}
}
