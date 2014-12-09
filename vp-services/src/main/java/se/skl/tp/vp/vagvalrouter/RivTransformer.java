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
package se.skl.tp.vp.vagvalrouter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;

/**
 * Transforms messages between RIVTABP20 and RIVTABP21 and vice versa.
 *
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class RivTransformer extends AbstractMessageTransformer {

	private static Logger log = LoggerFactory.getLogger(RivTransformer.class);

	private VisaVagvalsInterface vagvalAgent;

	static final String RIV20 = "RIVTABP20";
	static final String RIV21 = "RIVTABP21";

	static final String RIV20_NS = "http://www.w3.org/2005/08/addressing";
	static final String RIV20_ELEM = "To";

	static final String RIV21_NS = "urn:riv:itintegration:registry:1";
	static final String RIV21_ELEM = "LogicalAddress";

    private static XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

	public RivTransformer() {
		super();
	}

	public void setVagvalAgent(final VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	@Override
	public Object transformMessage(MuleMessage msg, String encoding) throws TransformerException {

		log.debug("Riv transformer executing");

		final AddressingHelper addrHelper = new AddressingHelper(msg, vagvalAgent);

		/*
		 * Check if virtualized service is a 2.0 service
		 */
		String rivVersion = (String) msg.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);

		/*
		 * Get the available RIV version from the service directory, and if the
		 * version doesn't match, update to the producer version and transform
		 */
		String rivProfile = "";
		try {
			rivProfile = addrHelper.getAvailableRivProfile();
		} catch (VpSemanticException e) {
			/*
			 * Set the exception in INVOVATION scoped property, to be able for
			 * VagvalRouter to pick it up.
			 */
			msg.setInvocationProperty(VPUtil.VP_SEMANTIC_EXCEPTION, e);
			return msg;
		}
		log.debug("RivProfile set to session scope: " + rivProfile);

		if (rivVersion.equalsIgnoreCase(RIV20) && rivProfile.equalsIgnoreCase(RIV21)) {
			this.doTransform(msg, RIV20_NS, RIV21_NS, RIV20_ELEM, RIV21_ELEM);
			msg.setProperty(VPUtil.RIV_VERSION, rivProfile, PropertyScope.SESSION);
		}

		if (rivVersion.equalsIgnoreCase(RIV21) && rivProfile.equalsIgnoreCase(RIV20)) {
			this.doTransform(msg, RIV21_NS, RIV20_NS, RIV21_ELEM, RIV20_ELEM);
			msg.setProperty(VPUtil.RIV_VERSION, rivProfile, PropertyScope.SESSION);
		}

		return msg;
	}

	Object doTransform(final MuleMessage msg, final String fromNs, final String toNs, final String fromElem,
			final String toElem) {

		log.info("Transforming {} -> {}. Payload is of type {}", new Object[] { fromNs, toNs,
				msg.getPayload().getClass().getName() });

		try {
			ReversibleXMLStreamReader reader = (ReversibleXMLStreamReader) msg.getPayload();

			final ByteArrayOutputStream newContents = transformXml(reader, fromNs, toNs, fromElem, toElem);

			msg.setPayload(new ReversibleXMLStreamReader(XMLInputFactory.newInstance().createXMLStreamReader(
					new ByteArrayInputStream(newContents.toByteArray()), "UTF-8")));

			return msg;
		} catch (final Exception e) {
			log.error("RIV transformation failed", e);
			throw new RuntimeException(e);
		}
	}

	// alternative
	static ByteArrayOutputStream transformXml(XMLStreamReader reader,
			final String fromAddressingNs,
			final String toAddressingNs, final String fromAddressingElement,
			final String toAddressingElement) throws XMLStreamException {

		log.debug("RivTransformer transformXML");

		ByteArrayOutputStream os = new ByteArrayOutputStream(2048);
		XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(os, "UTF-8");

	    writer.writeStartDocument();

		int read = 0;
		int event = reader.getEventType();

		while (reader.hasNext()) {
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				read++;
				writeStartElement(reader, writer, fromAddressingNs, toAddressingNs, fromAddressingElement, toAddressingElement);
				break;
			case XMLStreamConstants.END_ELEMENT:
				writer.writeEndElement();
				read--;
				if (read <= 0) {
				    writer.writeEndDocument();
					return os;
				}
				break;
			case XMLStreamConstants.CHARACTERS:
				writer.writeCharacters(reader.getText());
				break;
			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.END_DOCUMENT:
			case XMLStreamConstants.ATTRIBUTE:
			case XMLStreamConstants.NAMESPACE:
				break;
			case XMLStreamConstants.COMMENT:
				writer.writeComment(reader.getText());
				break;
			default:
				break;
			}
			event = reader.next();
		}
	    writer.writeEndDocument();

		return os;
	}

	private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer,
			final String fromAddressingNs,
			final String toAddressingNs,
			final String fromAddressingElement,
			final String toAddressingElement)
			throws XMLStreamException {

		String uri = reader.getNamespaceURI();
		if (fromAddressingNs.equals(uri)) {
			if (log.isDebugEnabled()) {
				log.debug("RivTransformer { fromNS: {}, toNS: {} }", new Object[] { fromAddressingNs, toAddressingNs });
			}
			uri = toAddressingNs;
		}

		String local = reader.getLocalName();
		// make sure we only transforms element names within the right namespace
		if (fromAddressingElement.equals(local) && toAddressingNs.equals(uri)) {
			local = toAddressingElement;
			if (log.isDebugEnabled()) {
				log.debug("RivTransformer { fromName: {}, toName: {}, uri: {} }", new Object[] { fromAddressingElement, toAddressingElement, uri });
			}
		}

		String prefix = reader.getPrefix();
		if (prefix == null) {
			prefix = "";
		}


		//System.out.println("STAXUTILS:writeStartElement : node name : " + local +  " namespace URI" + uri);
		boolean writeElementNS = false;
		if (uri != null) {
			String boundPrefix = writer.getPrefix(uri);
			if (boundPrefix == null || !prefix.equals(boundPrefix)) {
				writeElementNS = true;
			}
		}

		// Write out the element name
		if (uri != null) {
			if (prefix.length() == 0 && StringUtils.isEmpty(uri)) {
				writer.writeStartElement(local);
				writer.setDefaultNamespace(uri);

			} else {
				writer.writeStartElement(prefix, local, uri);
				writer.setPrefix(prefix, uri);
			}
		} else {
			writer.writeStartElement(local);
		}

		// Write out the namespaces
		for (int i = 0; i < reader.getNamespaceCount(); i++) {
			String nsURI = reader.getNamespaceURI(i);
			if (fromAddressingNs.equals(nsURI) && ("Envelope".equals(local) || "Header".equals(local)
					|| toAddressingElement.equals(local))) {
					nsURI = toAddressingNs;
			}

			String nsPrefix = reader.getNamespacePrefix(i);
			if (nsPrefix == null) {
				nsPrefix = "";
			}

			if (nsPrefix.length() == 0) {
				writer.writeDefaultNamespace(nsURI);
			} else {
				writer.writeNamespace(nsPrefix, nsURI);
			}

			if (nsURI.equals(uri) && nsPrefix.equals(prefix)) {
				writeElementNS = false;
			}
		}

		// Check if the namespace still needs to be written.
		// We need this check because namespace writing works
		// different on Woodstox and the RI.
		if (writeElementNS) {
			if (prefix.length() == 0) {
				writer.writeDefaultNamespace(uri);
			} else {
				writer.writeNamespace(prefix, uri);
			}
		}

		// Write out attributes
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String ns = reader.getAttributeNamespace(i);

			if (fromAddressingNs.equals(ns) && toAddressingElement.equals(local)) {
				ns = toAddressingNs;
			}

			String nsPrefix = reader.getAttributePrefix(i);
			if (ns == null || ns.length() == 0) {
				writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
			} else if (nsPrefix == null || nsPrefix.length() == 0) {
				writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
						reader.getAttributeValue(i));
			} else {
				writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader
						.getAttributeLocalName(i), reader.getAttributeValue(i));
			}

		}
	}
}
