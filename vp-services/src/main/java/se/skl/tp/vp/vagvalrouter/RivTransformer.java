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

import javax.xml.namespace.QName;
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
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.util.helper.PayloadHelper;

/**
 * Transforms messages between RIVTABP20 and RIVTABP21 and
 * vice versa.
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class RivTransformer extends AbstractMessageAwareTransformer {

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
	public Object transform(MuleMessage msg, String encoding)
			throws TransformerException {
		
		log.debug("Riv transformer executing");
		
		final PayloadHelper routerHelper = new PayloadHelper(msg);
		final AddressingHelper addrHelper = new AddressingHelper(msg, vagvalAgent, pattern, this.whiteList);
		
		/*
		 * Check if virtualized service is a
		 * 2.0 service
		 */
		String rivVersion = (String) msg.getProperty(VPUtil.RIV_VERSION);
		if (rivVersion == null || rivVersion.equals("")) {
			final String tns = VPUtil.extractNamespaceFromService((QName) msg.getProperty(VPUtil.SERVICE_NAMESPACE));
			final String[] split = tns.split(":");
			
			rivVersion = split[split.length - 1];
		}
		
		msg.setProperty(VPUtil.RIV_VERSION, rivVersion.toUpperCase());
		
		/*
		 * Get the receiver BEFORE any transformation
		 */
		msg.setProperty(VPUtil.RECEIVER_ID, routerHelper.extractReceiverFromPayload());
		
		/*
		 * Get the available RIV version from the service directory, and if the
		 * version doesn't match, update to the producer version and transform
		 */
		final String rivProfile = addrHelper.getAvailableRivProfile();
		msg.setProperty(VPUtil.RIV_VERSION, rivProfile);
		
		if (rivVersion.equalsIgnoreCase(RIV20) && rivProfile.equalsIgnoreCase(RIV21)) {
			this.doTransform(msg, RIV20_NS, RIV21_NS, RIV20_ELEM, RIV21_ELEM);
		}
		
		if (rivVersion.equalsIgnoreCase(RIV21) && rivProfile.equalsIgnoreCase(RIV20)) {
			this.doTransform(msg, RIV21_NS, RIV20_NS, RIV21_ELEM, RIV20_ELEM);
		}
		
		return msg;
	}
	
	Object doTransform(final MuleMessage msg, final String fromNs, final String toNs, final String fromElem, final String toElem) {
		
		log.info("Transforming {} -> {}. Payload is of type {}", new Object[] { fromNs, toNs, msg.getPayload().getClass().getName() });
		
		try {
			final ByteArrayOutputStream newContents = this.transformXml((ReversibleXMLStreamReader) msg.getPayload(), fromNs, toNs, fromElem, toElem);
			msg.setPayload(new ReversibleXMLStreamReader(
					XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(newContents.toByteArray()))));
			
			return msg;
		} catch (final Exception e) {
			log.error("Could not transform XML stream", e);
		}
		
		throw new IllegalStateException("Could not transform");
	}
	
	ByteArrayOutputStream transformXml(final XMLStreamReader originalReader
			, final String fromAddressingNs
			, final String toAddressingNs
			, final String fromAddressingElement
			, final String toAddressingElement) throws XMLStreamException {
		
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(originalReader);
		final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
		final XMLEventFactory factory = XMLEventFactory.newInstance();
		
		elementLoop : while (reader.hasNext()) {
			final XMLEvent event = reader.nextEvent();
			
			if (event.isStartElement()) {
				
				final StartElement se = event.asStartElement();
				
				if (se.getName().getLocalPart().equals("Envelope")) {
					
					final StartElement envelope = factory.createStartElement(se.getName().getPrefix(), se.getName().getNamespaceURI(), se.getName().getLocalPart());
					writer.add(envelope);
					
					/*
					 * Replace the namespace for ws-addressing
					 */
					@SuppressWarnings("rawtypes")
					final Iterator namespaces = se.getNamespaces();
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
					
					continue elementLoop;
				}
				
				if (se.getName().getLocalPart().equals(fromAddressingElement)) {
					/*
					 * Create a LogicalAddress
					 */
					StartElement newLogicalAddress = factory.createStartElement(se.getName().getPrefix(), toAddressingNs, toAddressingElement);
					writer.add(newLogicalAddress);
					
					continue elementLoop;
				}
			}
				
				
			if (event.isEndElement()) {
				final EndElement ee = event.asEndElement();
				
				if (ee.getName().getLocalPart().equals(fromAddressingElement)) {
					final EndElement newEndElement = factory.createEndElement(ee.getName().getPrefix(), toAddressingNs, toAddressingElement);
					writer.add(newEndElement);
					
					continue elementLoop;
				}
			}
			
			writer.add(event);
		}
		
		writer.flush();
		return out;
	}
}
