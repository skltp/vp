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
package se.skl.tp.vp.util;

import java.io.UnsupportedEncodingException;

import javax.jms.Message;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.transport.jms.JmsMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import org.soitoolkit.commons.mule.util.XmlUtil;

/**
 * Transforms payload to string.
 * 
 * @since VP 2.0
 * @author Peter
 */
public class PayloadToStringTransformer {
	private JaxbObjectToXmlTransformer jaxbToXml;

	private static final Logger log = LoggerFactory.getLogger(PayloadToStringTransformer.class);

	//
	public PayloadToStringTransformer(JaxbObjectToXmlTransformer jaxbToXml) {
		this.jaxbToXml = jaxbToXml;
	}

	//
	public JaxbObjectToXmlTransformer getJaxbObjectToXmlTransformer() {
		return this.jaxbToXml;
	}

	//
	public String getPayloadAsString(Object payload) {
		if (payload == null) {
			return null;
		}

		String content;
		if (payload instanceof Object[]) {
			StringBuilder buf = new StringBuilder();
			int i = 0;
			for (Object object : (Object[]) payload) {
				if (i > 0) {
					buf.append("\n");
				}
				buf.append("[").append(i++).append("]: ").append(getContentAsString(object));
			}
			content = buf.toString();
		} else {
			content = getContentAsString(payload);
		}

		return content;
	}

	//
	private String getContentAsString(Object payload) {
		if (payload == null) {
			return null;
		}
		String content;
		if (payload instanceof String) {
			content = (String) payload;
		} else if (payload instanceof DepthXMLStreamReader) {
			final DepthXMLStreamReader dp = (DepthXMLStreamReader) payload;
			content = XmlUtil.convertXMLStreamReaderToString(dp, "UTF-8");
		} else if (payload instanceof byte[]) {			
			try {
				content = new String((byte[])payload, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				content = payload.getClass().getName();
				log.warn("Unexpected error while converting byte array to string (ignored)", e);
			}
		} else if (payload instanceof ReversibleXMLStreamReader) {
			content = XmlUtil.convertReversibleXMLStreamReaderToString(
					(ReversibleXMLStreamReader) payload, "UTF-8");
		} else if (payload instanceof Message) {
			content = convertJmsMessageToString(payload, "UTF-8");
		} else if (isJabxObject(payload)) {
			content = getJaxbContentAsString(payload, "UTF-8");
		} else {
			content = payload.getClass().getName();
			if (log.isDebugEnabled()) {
				log.debug("Unable to convert payload of type {} to string", content);
			}
		}
		return content;
	}


	//
	private static String convertJmsMessageToString(Object payload, String outputEncoding) {
		try {
			return JmsMessageUtils.toObject((Message) payload, null,
					outputEncoding).toString();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}


	//
	private static boolean isJabxObject(Object payload) {
		return payload.getClass().isAnnotationPresent(XmlType.class);
	}

	//
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getJaxbContentAsString(Object jaxbObject, String outputEncoding) {
		String content;
		if (this.jaxbToXml == null) {
			log.error("Missing jaxb2xml injection, can't marshal JAXB objects");
			content = "Missing jaxb2xml injection, can't marshal JAXB object of type: "
					+ jaxbObject.getClass().getName();
		} else {
			JAXBElement wrapped = null;
			if (!jaxbObject.getClass()
					.isAnnotationPresent(XmlRootElement.class)) {
				// We are missing element end namespace info, let's create a
				// wrapper xml-root-element
				QName wrapperQName = new QName("class:"
						+ jaxbObject.getClass().getName(),
						getJaxbWrapperElementName(jaxbObject));
				wrapped = new JAXBElement(wrapperQName, jaxbObject.getClass(), null, jaxbObject);

				log.info("Created root wrapper: {}", wrapped);
			}

			try {
				content = (String) this.jaxbToXml.doTransform(wrapped, outputEncoding);
			} catch (TransformerException e) {
				log.error("JAXB object marshalling failed", e);
				content = "JAXB object marshalling failed: " + e.getMessage();
			}
		}

		return content;
	}

	//
	private static String getJaxbWrapperElementName(Object payload) {
		String name = payload.getClass().getSimpleName();
		String elementName = name.substring(0, 1).toLowerCase()
				+ name.substring(1);
		return elementName;
	}

}