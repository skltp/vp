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
package se.skl.tp.vp.util.helper;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.mule.api.MuleMessage;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import se.skl.tp.vp.exceptions.VpTechnicalException;

/**
 * Helper class for working with the
 * payload of messages
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class PayloadHelper extends VPHelperSupport {

	public PayloadHelper(MuleMessage muleMessage) {
		super(muleMessage);
	}

	/**
	 * Get the receiver from the payload.
	 * 
	 * @return the receiver or null if payload can't be parsed.
	 */
	public String extractReceiverFromPayload() {
		Object payload = getMuleMessage().getPayload();
		if (!(payload instanceof ReversibleXMLStreamReader)) {
			this.getLog().warn("Unable to extract important RIV information (receiverid): { payload: {} }", payload);
			return null;
		}
		ReversibleXMLStreamReader reader = (ReversibleXMLStreamReader) payload;

		// Start caching events from the XML documents
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug("Start caching events from the XML docuement parsing");
		}

		reader.setTracking(true);

		try {
			return this.parsePayloadForReceiver(reader);
		} catch (final XMLStreamException e) {
			throw new VpTechnicalException(e);
		} finally {
			// Go back to the beginning of the XML document
			if (this.getLog().isDebugEnabled()) {
				this.getLog().debug("Go back to the beginning of the XML document");
			}
			reader.reset();
		}
	}

	private String parsePayloadForReceiver(final ReversibleXMLStreamReader reader) throws XMLStreamException {
		String receiverId = null;
		boolean headerFound = false;
		boolean bodyFound = false;
		
		int event = reader.getEventType();

		while (reader.hasNext()) {
			switch (event) {

			case XMLStreamConstants.START_ELEMENT:
				String local = reader.getLocalName();
				
//				System.err.println(local);
//				
//				if(bodyFound){
//					System.err.println(local);
//					System.err.println(reader.getNamespaceURI());
//					
//					return receiverId;
//				}
//				
//				if (local.equals("Body")) {
//					bodyFound = true;
//				}

				if (local.equals("Header")) {
					headerFound = true;
				}

				// Don't bother about riv-version in this code
				if (headerFound && (local.equals("To") || local.equals("LogicalAddress"))) {
					reader.next();
					receiverId = reader.getText();
					if (this.getLog().isDebugEnabled()) {
						this.getLog().debug("found To in Header= " + receiverId);
					}
				}

				break;

			case XMLStreamConstants.END_ELEMENT:
				if (reader.getLocalName().equals("Header")) {
					// We have found the end element of the Header, i.e. we
					// are done. Let's bail out!
					if (this.getLog().isDebugEnabled()) {
						this.getLog().debug("We have found the end element of the Header, i.e. we are done.");
					}
					return receiverId;
				}
				break;

			case XMLStreamConstants.CHARACTERS:
				break;

			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.END_DOCUMENT:
			case XMLStreamConstants.ATTRIBUTE:
			case XMLStreamConstants.NAMESPACE:
				break;

			default:
				break;
			}
			event = reader.next();
		}

		return receiverId;
	}
}
