/**
 * Copyright 2011 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalrouter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.util.helper.PayloadHelper;

/**
 * Transforms messages between RIVTABP20 and RIVTABP21 and vice versa.
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class RivTransformer extends AbstractMessageTransformer {

	private static Logger log = LoggerFactory.getLogger(RivTransformer.class);

	private VisaVagvalsInterface vagvalAgent;
	private Pattern pattern;
	private String senderIdPropertyName;

	private String whiteList;

	static final String RIV20 = "RIVTABP20";
	static final String RIV21 = "RIVTABP21";

	static final String RIV20_NS = "http://www.w3.org/2005/08/addressing";
	static final String RIV20_ELEM = "To";

	static final String RIV21_NS = "urn:riv:itintegration:registry:1";
	static final String RIV21_ELEM = "LogicalAddress";

	public void setWhiteList(final String whiteList) {
		this.whiteList = whiteList;
	}

	public void setVagvalAgent(final VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	public void setSenderIdPropertyName(final String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		this.pattern = Pattern.compile(this.senderIdPropertyName + "=([^,]+)");
		if (logger.isDebugEnabled()) {
			logger.debug("senderIdPropertyName set to: " + senderIdPropertyName);
		}
	}

	@Override
	public Object transformMessage(MuleMessage msg, String encoding) throws TransformerException {

		log.debug("Riv transformer executing");

		final PayloadHelper routerHelper = new PayloadHelper(msg);
		final AddressingHelper addrHelper = new AddressingHelper(msg, vagvalAgent, pattern, this.whiteList);

		/*
		 * Check if virtualized service is a 2.0 service
		 */
		String rivVersion = (String) msg.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);

		/*
		 * Get the receiver BEFORE any transformation
		 */
		msg.setProperty(VPUtil.RECEIVER_ID, routerHelper.extractReceiverFromPayload(), PropertyScope.SESSION);

		/*
		 * Get the available RIV version from the service directory, and if the
		 * version doesn't match, update to the producer version and transform
		 */
		final String rivProfile = addrHelper.getAvailableRivProfile();
		msg.setProperty(VPUtil.RIV_VERSION, rivProfile, PropertyScope.SESSION);

		if (rivVersion.equalsIgnoreCase(RIV20) && rivProfile.equalsIgnoreCase(RIV21)) {
			this.doTransform(msg, RIV20_NS, RIV21_NS, RIV20_ELEM, RIV21_ELEM);
		}

		if (rivVersion.equalsIgnoreCase(RIV21) && rivProfile.equalsIgnoreCase(RIV20)) {
			this.doTransform(msg, RIV21_NS, RIV20_NS, RIV21_ELEM, RIV20_ELEM);
		}

		return msg;
	}

	Object doTransform(final MuleMessage msg, final String fromNs, final String toNs, final String fromElem,
			final String toElem) {

		log.info("Transforming {} -> {}. Payload is of type {}", new Object[] { fromNs, toNs,
				msg.getPayload().getClass().getName() });

		try {
			final ByteArrayOutputStream newContents = this.transformXml((ReversibleXMLStreamReader) msg.getPayload(),
					fromNs, toNs, fromElem, toElem);
			msg.setPayload(new ReversibleXMLStreamReader(XMLInputFactory.newInstance().createXMLStreamReader(
					new ByteArrayInputStream(newContents.toByteArray()))));

			return msg;
		} catch (final Exception e) {
			log.error("Could not transform XML stream", e);
		}

		throw new IllegalStateException("Could not transform");
	}

	ByteArrayOutputStream transformXml(final XMLStreamReader originalReader, final String fromAddressingNs,
			final String toAddressingNs, final String fromAddressingElement, final String toAddressingElement)
			throws XMLStreamException {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(originalReader);
		final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
		final XMLEventFactory factory = XMLEventFactory.newInstance();

		elementLoop: while (reader.hasNext()) {
			final XMLEvent event = reader.nextEvent();

			if (event.isStartElement()) {
				final StartElement startElement = event.asStartElement();

				if (isEnvelopeElement(startElement)) {
					addStartElement(writer, factory, startElement);
					replaceNamespacesForAddressingElement(fromAddressingNs, toAddressingNs, writer, factory,
							startElement);
					continue elementLoop;
				}

				if (isHeaderElement(startElement)) {
					addStartElement(writer, factory, startElement);
					replaceNamespacesForAddressingElement(fromAddressingNs, toAddressingNs, writer, factory,
							startElement);
					continue elementLoop;
				}

				if (isAdressingElement(fromAddressingElement, startElement)) {
					replaceAddressingElement(toAddressingNs, toAddressingElement, writer, factory, startElement);
					replaceNamespacesForAddressingElement(fromAddressingNs, toAddressingNs, writer, factory,
							startElement);
					continue elementLoop;
				}
			}

			if (event.isEndElement()) {
				final EndElement endElement = event.asEndElement();
				if (isAddressingElement(fromAddressingElement, endElement)) {
					addEndElement(toAddressingNs, toAddressingElement, writer, factory, endElement);
					continue elementLoop;
				}
			}
			writer.add(event);
		}

		writer.flush();
		return out;
	}

	private boolean isHeaderElement(StartElement startElement) {
		return startElement.getName().getLocalPart().equals("Header");
	}

	private boolean isAddressingElement(final String fromAddressingElement, final EndElement endElement) {
		return endElement.getName().getLocalPart().equals(fromAddressingElement);
	}

	private boolean isAdressingElement(final String fromAddressingElement, final StartElement startElement) {
		return startElement.getName().getLocalPart().equals(fromAddressingElement);
	}

	private boolean isEnvelopeElement(final StartElement startElement) {
		return startElement.getName().getLocalPart().equals("Envelope");
	}

	private void replaceAddressingElement(final String toAddressingNs, final String toAddressingElement,
			final XMLEventWriter writer, final XMLEventFactory factory, final StartElement se)
			throws XMLStreamException {
		StartElement newLogicalAddress = factory.createStartElement(se.getName().getPrefix(), toAddressingNs,
				toAddressingElement);
		writer.add(newLogicalAddress);
	}

	private void addStartElement(final XMLEventWriter writer, final XMLEventFactory factory, final StartElement se)
			throws XMLStreamException {
		final StartElement envelope = factory.createStartElement(se.getName().getPrefix(), se.getName()
				.getNamespaceURI(), se.getName().getLocalPart());
		writer.add(envelope);
	}

	private void addEndElement(final String toAddressingNs, final String toAddressingElement,
			final XMLEventWriter writer, final XMLEventFactory factory, final EndElement endElement)
			throws XMLStreamException {
		final EndElement newEndElement = factory.createEndElement(endElement.getName().getPrefix(), toAddressingNs,
				toAddressingElement);
		writer.add(newEndElement);
	}

	private void replaceNamespacesForAddressingElement(final String fromAddressingNs, final String toAddressingNs,
			final XMLEventWriter writer, final XMLEventFactory factory, final StartElement se)
			throws XMLStreamException {

		@SuppressWarnings("unchecked")
		final Iterator<Namespace> namespaces = se.getNamespaces();
		while (namespaces.hasNext()) {
			Namespace ns = (Namespace) namespaces.next();

			if (ns.getValue().equals(fromAddressingNs)) {
				final Namespace namespace = factory.createNamespace(ns.getPrefix(), toAddressingNs);
				writer.add(namespace);
			} else {
				final Namespace namespace = factory.createNamespace(ns.getPrefix(), ns.getNamespaceURI());
				writer.add(namespace);
			}
		}
	}
}
