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
	 * Get the info from the payload.
	 * 
	 * @return ReciverId, service contract namespace or null if not found
	 */
	public PayloadInfo extractInfoFromPayload() {
		Object payload = getMuleMessage().getPayload();
		if (!(payload instanceof ReversibleXMLStreamReader)) {
			this.getLog().warn("Payload not xmlstream! Unable to extract important RIV information (receiverid, service contract namespace): { payload: {} }", payload);
			return new PayloadInfo();
		}
		ReversibleXMLStreamReader reader = (ReversibleXMLStreamReader) payload;

		// Start caching events from the XML documents
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug("Start caching events from the XML docuement parsing");
		}

		reader.setTracking(true);

		try {
			return this.parsePayloadForInfo(reader);
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

	private PayloadInfo parsePayloadForInfo(final ReversibleXMLStreamReader reader) throws XMLStreamException {
		PayloadInfo payloadInfo = new PayloadInfo();
		boolean headerFound = false;
		boolean bodyFound = false;
		
		int event = reader.getEventType();

		while (reader.hasNext()) {
			switch (event) {

			case XMLStreamConstants.START_ELEMENT:
				String local = reader.getLocalName();

//				Based on convention that payload is in following format
//				
//				<soapenv:Header>
//					<urn:Actor>
//						<urn:actorId>?</urn:actorId>
//						<urn:actorType>?</urn:actorType>
//					</urn:Actor>
//					<urn1:LogicalAddress>HSA-VKK123</urn1:LogicalAddress>
//				</soapenv:Header>
//				<soapenv:Body>
//					<urn2:GetSubjectOfCareSchedule>
//						<urn2:healthcare_facility>HSA-VKK123</urn2:healthcare_facility>
//						<urn2:subject_of_care>188803099368</urn2:subject_of_care>
//					</urn2:GetSubjectOfCareSchedule>
//				</soapenv:Body>
				
				
				if(bodyFound){
					// We have found the element we need in the Header and Body, i.e. we
					// are done. Let's bail out!
					payloadInfo.serviceContractNamespace = reader.getNamespaceURI();
					
					if(getLog().isDebugEnabled()){
						getLog().debug("Payload parsed for information, return result: " + payloadInfo);
					}
					return payloadInfo;
				}
				
				//Body found, next element is the service interaction e.g GetSubjectOfCareSchedule
				if (local.equals("Body")) {
					bodyFound = true;
					break; //Break to the next element, the service interaction handled above
				}

				if (local.equals("Header")) {
					headerFound = true;
				}

				// Don't bother about riv-version in this code
				if (headerFound && (local.equals("To") || local.equals("LogicalAddress"))) {
					reader.next();
					payloadInfo.receiverId = reader.getText();
				}

				break;

			case XMLStreamConstants.END_ELEMENT:
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

		return payloadInfo;
	}
	
	/*
	 * Contains information extracted from payload
	 */
	public class PayloadInfo{
		String serviceContractNamespace;
		String receiverId;
		public String getServiceContractNamespace() {
			return serviceContractNamespace;
		}
		public String getReceiverId() {
			return receiverId;
		}
		@Override
		public String toString() {
			return "ReceiverId: " + receiverId + ", " +
					"Service contract namespace: " + serviceContractNamespace;
		}
	}
}
