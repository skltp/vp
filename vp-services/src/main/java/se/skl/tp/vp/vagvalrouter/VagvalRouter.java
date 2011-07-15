/**
 * Copyright 2009 Sjukvardsradgivningen
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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.routing.outbound.AbstractRecipientList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.dashboard.ServiceStatistics;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;

public class VagvalRouter extends AbstractRecipientList {

	private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	public static String RECEIVER_ID = "receiverid";
	public static String SENDER_ID = "senderid";
	public static String RIV_VERSION = "rivversion";
	public static String SERVICE_NAMESPACE = "namespace";

	private VisaVagvalsInterface vagvalAgent;

	private Pattern pattern;

	private String senderIdPropertyName;

	private Map<String, ServiceStatistics> statistics = new HashMap<String, ServiceStatistics>();

	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(senderIdPropertyName + "=([^,]+)");
		if (logger.isInfoEnabled()) {
			logger.info("senderIdPropertyName set to: " + senderIdPropertyName);
		}
	}

	// Not private to make the method testable...
	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List getRecipients(MuleMessage message) throws CouldNotRouteOutboundMessageException {

		String address = getAddress(message);
		List<String> list = new ArrayList<String>();
		list.add(address);

		if (logger.isDebugEnabled()) {
			logger.debug("VagvalRouter directs call at serviceNamespace: "
					+ message.getProperty(SERVICE_NAMESPACE) + ", receiverId: "
					+ message.getProperty(RECEIVER_ID) + ", senderId: "
					+ message.getProperty(SENDER_ID) + " rivVersion: "
					+ message.getProperty(RIV_VERSION) + "to adress: " + address);
		}
		return list;
	}

	/**
	 * Override this method to be able to collect statistics per
	 * Contract/Service Producer
	 */
	@Override
	public MuleMessage route(MuleMessage message, MuleSession session) throws RoutingException {

		String receiverId = getReceiverId(message);
		message.setProperty(RECEIVER_ID, receiverId);

		long beforeCall = System.currentTimeMillis();
		String serviceId = message.getProperty(SERVICE_NAMESPACE) + "-"
				+ message.getProperty(RECEIVER_ID);

		synchronized (statistics) {

			if (statistics.keySet().size() == 0) {
				try {
					muleContext.getRegistry().registerObject("tp-statistics", statistics);
				} catch (RegistrationException e) {
					logger.error("Stats not possible to register" + e);
					// Dont let this interfere with the actual processing of
					// messages
				}
			}

			ServiceStatistics serverStatistics = statistics.get(serviceId);
			if (serverStatistics == null) {
				serverStatistics = new ServiceStatistics();
				serverStatistics.producerId = serviceId;
				statistics.put(serviceId, serverStatistics);
			}
			serverStatistics.noOfCalls++;
		}

		// Do the actual routing
		MuleMessage replyMessage = super.route(message, session);

		synchronized (statistics) {
			ServiceStatistics serverStatistics = statistics.get(serviceId);
			serverStatistics.noOfSuccesfullCalls++;
			long duration = System.currentTimeMillis() - beforeCall;
			serverStatistics.totalDuration += duration;
			serverStatistics.averageDuration = serverStatistics.totalDuration
					/ serverStatistics.noOfSuccesfullCalls;
		}
		return replyMessage;
	}

	@Override
	protected OutboundEndpoint getRecipientEndpoint(MuleMessage message, Object recipient)
			throws RoutingException {

		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder((String) recipient),
				muleContext);
		eb.setSynchronous(true);
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("proxy", "true");
		properties.put("payload", "envelope");
		eb.setProperties(properties);

		try {
			return eb.buildOutboundEndpoint();
		} catch (InitialisationException e) {
			throw new VpTechnicalException(e);
		} catch (EndpointException e) {
			throw new VpTechnicalException(e);
		}
	}


	/**
	 * Returns the address determined by input data (senderId, receiverId, RIV version and namespace)
	 * 
	 * @param message
	 * @return
	 */
	String getAddress(MuleMessage message) {

		VagvalInput vagvalInput = new VagvalInput();

		vagvalInput.senderId = VPUtil.getSenderIdFromCertificate(message, this.pattern);
		message.setProperty(SENDER_ID, vagvalInput.senderId);

		vagvalInput.receiverId = message.getProperty(RECEIVER_ID).toString();
		vagvalInput.rivVersion = message.getProperty(RIV_VERSION).toString();
		vagvalInput.serviceNamespace = message.getProperty(SERVICE_NAMESPACE).toString();

		return getAddressFromAgent(vagvalInput);

	}

	/**
	 * Not private to make the method testable...
	 * 
	 * @param serviceNamespace
	 * @param vagvalInput
	 * @return
	 */
	String getAddressFromAgent(VagvalInput vagvalInput) {
		if (logger.isDebugEnabled()) {
			logger.debug("Calling vagvalAgent with serviceNamespace:"
					+ vagvalInput.serviceNamespace + ", receiverId:" + vagvalInput.receiverId
					+ ", senderId: " + vagvalInput.senderId);
		}
		if (vagvalInput.rivVersion == null) {
			String errorMessage = ("VP001 No Riv-version configured in tp-virtuell-tjanst-config, transformer-refs with property name 'rivversion' missing");
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (vagvalInput.senderId == null) {
			String errorMessage = ("VP002 No senderId found in Certificate");
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (vagvalInput.receiverId == null) {
			String errorMessage = ("VP003 No receiverId found in RivHeader");
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(
				vagvalInput.senderId, vagvalInput.receiverId, vagvalInput.serviceNamespace));

		List<VirtualiseringsInfoType> virtualiseringar = vvResponse.getVirtualiseringsInfo();
		if (logger.isDebugEnabled()) {
			logger.debug("VagvalAgent response count: " + virtualiseringar.size());
			for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
				logger.debug("VagvalAgent response item RivProfil: " + vvInfo.getRivProfil()
						+ ", Address: " + vvInfo.getAdress());
			}
		}
		if (virtualiseringar.size() == 0) {
			String errorMessage = "VP004 No Logical Adress found for serviceNamespace:"
					+ vagvalInput.serviceNamespace + ", receiverId:" + vagvalInput.receiverId;
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		String adress = null;
		int noOfMatchingAdresses = 0;
		for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
			if (vvInfo.getRivProfil().equals(vagvalInput.rivVersion)) {
				adress = vvInfo.getAdress();
				noOfMatchingAdresses++;
			}
		}

		if (noOfMatchingAdresses == 0) {
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ vagvalInput.serviceNamespace
					+ ", receiverId:"
					+ vagvalInput.receiverId
					+ "RivVersion" + vagvalInput.rivVersion);
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (noOfMatchingAdresses > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ vagvalInput.serviceNamespace + ", receiverId:" + vagvalInput.receiverId;
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = ("VP010 Physical Adress field is empty in Service Producer for serviceNamespace :"
					+ vagvalInput.serviceNamespace
					+ ", receiverId:"
					+ vagvalInput.receiverId
					+ "RivVersion" + vagvalInput.rivVersion);
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		adress = "cxf:" + adress;

		return adress;
	}
	
	
	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId,
			String tjansteGranssnitt) {
		VisaVagvalRequest vvR = new VisaVagvalRequest();

		vvR.setSenderId(senderId);

		vvR.setReceiverId(receiverId);

		vvR.setTjanstegranssnitt(tjansteGranssnitt);

		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		vvR.setTidpunkt(tidPunkt);

		return vvR;
	}

	/**
	 * Returns the elements from the RIV Header that are required by the
	 * VagvalAgent.
	 * 
	 * @param message
	 * @return
	 */
	private String getReceiverId(MuleMessage message) {

		Object payload = message.getPayload();
		//DepthXMLStreamReader dxsr = (DepthXMLStreamReader) payload;
		ReversibleXMLStreamReader rxsr = (ReversibleXMLStreamReader) payload;//dxsr.getReader();

		// Start caching events from the XML documents
		if (logger.isDebugEnabled()) {
			logger.debug("Start caching events from the XML docuement parsing");
		}
		rxsr.setTracking(true);

		try {

			return doGetReceiverIdFromPayload(rxsr);

		} catch (XMLStreamException e) {
			throw new VpTechnicalException(e);

		} finally {
			// Go back to the beginning of the XML document
			if (logger.isDebugEnabled()) {
				logger.debug("Go back to the beginning of the XML document");
			}
			rxsr.reset();
		}
	}

	/**
	 * Uses the StAX - API to get the elements from the SOAP Header.
	 * 
	 * @param reader
	 * @return
	 * @throws XMLStreamException
	 */
	private String doGetReceiverIdFromPayload(XMLStreamReader reader)
			throws XMLStreamException {

		String receiverId = null;
		boolean headerFound = false;

		int event = reader.getEventType();

		while (reader.hasNext()) {
			switch (event) {
			
			case XMLStreamConstants.START_ELEMENT:
				String local = reader.getLocalName();

				if (local.equals("Header")) {
					headerFound = true;
				}

				if (local.equals("To") && headerFound) {
					reader.next();
					receiverId = reader.getText();
					if (logger.isDebugEnabled()) {
						logger.debug("found To in Header= " + receiverId);
					}
				}

				break;

			case XMLStreamConstants.END_ELEMENT:
				if (reader.getLocalName().equals("Header")) {
					// We have found the end element of the Header, i.e. we
					// are done. Let's bail out!
					if (logger.isDebugEnabled()) {
						logger.debug("We have found the end element of the Header, i.e. we are done.");
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
