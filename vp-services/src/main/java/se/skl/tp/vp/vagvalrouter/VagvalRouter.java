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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.outbound.AbstractRecipientList;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.dashboard.ServiceStatistics;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.util.helper.PayloadHelper;

public class VagvalRouter extends AbstractRecipientList {

	private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	private VisaVagvalsInterface vagvalAgent;

	private Pattern pattern;

	private String senderIdPropertyName;

	private String whiteList;

	private String responseTimeout;

	private Map<String, ServiceStatistics> statistics = new HashMap<String, ServiceStatistics>();

	private AddressingHelper addrHelper;

	void setAddressingHelper(final AddressingHelper helper) {
		this.addrHelper = helper;
	}

	public AddressingHelper getAddressingHelper(final MuleMessage msg) {

		if (this.addrHelper == null) {
			this.setAddressingHelper(new AddressingHelper(msg, vagvalAgent, pattern, whiteList));
		}

		if (!this.addrHelper.getMuleMessage().equals(msg)) {
			this.setAddressingHelper(new AddressingHelper(msg, vagvalAgent, pattern, whiteList));
		}

		return this.addrHelper;
	}

	public void setWhiteList(final String whiteList) {
		this.whiteList = whiteList;
	}

	public void setResponseTimeout(final String responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(this.senderIdPropertyName + "=([^,]+)");
		if (logger.isInfoEnabled()) {
			logger.info("senderIdPropertyName set to: " + senderIdPropertyName);
		}
	}

	// Not private to make the method testable...
	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	@Override
	protected List<Object >getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException {
		final String addr = this.getAddressingHelper(event.getMessage()).getAddress();
		event.getMessage().setOutboundProperty(VPUtil.IS_HTTPS, addr.contains("https") ? true : false);

		return Collections.singletonList((Object)addr);
	}
	
	/**
	 * Override this method to be able to collect statistics per
	 * Contract/Service Producer
	 */
	@Override
    public MuleEvent route(MuleEvent event) throws RoutingException {

		final PayloadHelper routerHelper = new PayloadHelper(event.getMessage());

		final String receiverId = routerHelper.extractReceiverFromPayload();
		event.getMessage().setProperty(VPUtil.RECEIVER_ID, receiverId, PropertyScope.INVOCATION);

		long beforeCall = System.currentTimeMillis();
		String serviceId = VPUtil.extractNamespaceFromService((QName) event.getMessage().getProperty(VPUtil.SERVICE_NAMESPACE, PropertyScope.INVOCATION))
				+ "-" + event.getMessage().getProperty(VPUtil.RECEIVER_ID, PropertyScope.INVOCATION);

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
		MuleEvent replyEvent = super.route(event);

		/*
		 * Restore properties
		 */
		for (final Object prop : event.getMessage().getPropertyNames(PropertyScope.INVOCATION)) {
			replyEvent.getMessage().setProperty((String) prop, event.getMessage().getProperty((String) prop, PropertyScope.INVOCATION), PropertyScope.INVOCATION);
		}

		synchronized (statistics) {
			ServiceStatistics serverStatistics = statistics.get(serviceId);
			serverStatistics.noOfSuccesfullCalls++;
			long duration = System.currentTimeMillis() - beforeCall;
			serverStatistics.totalDuration += duration;
			serverStatistics.averageDuration = serverStatistics.totalDuration / serverStatistics.noOfSuccesfullCalls;
		}

		return replyEvent;
	}

	@Override
	protected OutboundEndpoint getRecipientEndpoint(MuleMessage message, Object recipient) throws RoutingException {

		
		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder((String) recipient, muleContext));
		eb.setResponseTimeout(Integer.valueOf(this.responseTimeout));

		setOutboundTransformers(eb);

		HashMap<Object, Object> properties = new HashMap<Object, Object>();
		properties.put("proxy", "true");
		properties.put("payload", "envelope");
		if (message.getOutboundProperty(VPUtil.IS_HTTPS, false)) {
			properties.put("protocolConnector", VPUtil.CONSUMER_CONNECTOR_NAME);
			logger.debug("Https protocolConnector set");
		}

		eb.setProperties(properties);

		try {
			return eb.buildOutboundEndpoint();
		} catch (InitialisationException e) {
			throw new VpTechnicalException(e);
		} catch (EndpointException e) {
			throw new VpTechnicalException(e);
		}
	}

	/*
	 * TP forwards properties in mule header that should not be forwarded. In
	 * the case the producer is another instance of TP (serivce platform) this
	 * can be problematic.
	 * 
	 * <message-properties-transformer name="deleteMuleHeaders">
	 * <delete-message-property key="x-vp-auth-cert"/>
	 * </message-properties-transformer>
	 */
	private void setOutboundTransformers(EndpointBuilder eb) {
		logger.info("Set outbound message transformers to update/add/remove mule message properties");

		MessagePropertiesTransformer transformer = new MessagePropertiesTransformer();
		transformer.setMuleContext(muleContext);
		transformer.setDeleteProperties(new ArrayList<String>());
		transformer.setAddProperties(new HashMap<String, Object>());

		handleContentTypeHeaders(transformer);
		handleReverseproxyHeaders(transformer);
		handleMuleHeadersNotToBePropagated(transformer);

		eb.addMessageProcessor(transformer);
	}

	private void handleContentTypeHeaders(MessagePropertiesTransformer transformer) {
		logger.debug("Remove any existing content-type and set content-type=text/xml;charset=UTF-8 on outbound endpoint");

		transformer.getDeleteProperties().add("content-type");
		transformer.getDeleteProperties().add("Content-Type");
		transformer.getDeleteProperties().add("Content-type");
		transformer.getDeleteProperties().add("content-Type");

		transformer.getAddProperties().put("Content-Type", "text/xml;charset=UTF-8");
	}

	private void handleReverseproxyHeaders(MessagePropertiesTransformer transformer) {
		logger.debug("Remove reverse proxy header information on outbound endpoint");
		transformer.getDeleteProperties().add(VPUtil.REVERSE_PROXY_HEADER_NAME);
	}

	private void handleMuleHeadersNotToBePropagated(MessagePropertiesTransformer transformer) {
		logger.debug("Remove mule header information not to be propagated on outbound endpoint");
		transformer.getDeleteProperties().add(VPUtil.SENDER_ID);
		//transformer.getDeleteProperties().add(VPUtil.RECEIVER_ID);
		transformer.getDeleteProperties().add(VPUtil.RIV_VERSION);
		transformer.getDeleteProperties().add(VPUtil.SERVICE_NAMESPACE);
		transformer.getDeleteProperties().add("namespace");
	}


}
