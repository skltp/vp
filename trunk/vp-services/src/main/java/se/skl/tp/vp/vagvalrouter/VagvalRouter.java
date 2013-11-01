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

import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_CORRELATION_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.GetMethod;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.outbound.AbstractRecipientList;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transport.http.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.dashboard.ServiceStatistics;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;

public class VagvalRouter extends AbstractRecipientList {

    /**
     * HTTP Header holding producer response time, forwarded to consumer.
     * <p>
     * 
     * @since VP-2.2.1
     */
    public static final String X_SKLTP_PRODUCER_RESPONSETIME = "x-skltp-prt";

    /**
	 * HTTP Header forwarded to producer
	 * <p>
	 * 
	 * @since VP-2.0
	 */
	public static final String X_VP_PRODUCER_ID = "x-vp-producer-id";

	/**
	 * HTTP Header forwarded to producer.
	 * <p>
	 * 
	 * @since VP-2.0
	 */
	public static final String X_VP_CONSUMER_ID = "x-rivta-original-serviceconsumer-hsaid";

	/**
	 * HTTP Header forwarded to consumer
	 * 
	 * @since VP-2.0
	 */
	public static final String X_VP_CORRELATION_ID = "x-vp-correlation-id";

	private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	private VisaVagvalsInterface vagvalAgent;

	private Pattern pattern;

	private String senderIdPropertyName;

	private String whiteList;

	private int responseTimeout;

	private Map<String, ServiceStatistics> statistics = new HashMap<String, ServiceStatistics>();

	private AddressingHelper addrHelper;

	/**
	 * Headers to be blocked when invoking producer.
	 */
	private static final List<String> BLOCKED_REQ_HEADERS = Collections.unmodifiableList(Arrays.asList(new String[] {
			VPUtil.SENDER_ID, VPUtil.RIV_VERSION, VPUtil.SERVICE_NAMESPACE, VPUtil.REVERSE_PROXY_HEADER_NAME,
			VPUtil.PEER_CERTIFICATES, "LOCAL_CERTIFICATES", HttpConstants.HEADER_CONTENT_TYPE,
			"http.disable.status.code.exception.check", }));

	/**
	 * Headers to be added when invoking producer.
	 */
	private static final Map<String, Object> ADD_HEADERS;
	static {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(HttpConstants.HEADER_USER_AGENT, "SKLTP VP/2.0");
		map.put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
		ADD_HEADERS = Collections.unmodifiableMap(map);
	}

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

	public void setResponseTimeout(final int responseTimeout) {
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
	protected List<Object> getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException {
		ExecutionTimer.start(VPUtil.TIMER_ROUTE);
		try {
			String addr = this.getAddressingHelper(event.getMessage()).getAddress();

			logger.debug("Endpoint address is {}", addr);

			return Collections.singletonList((Object) addr);
		} finally {
			ExecutionTimer.stop(VPUtil.TIMER_ROUTE);
		}
	}

	/**
	 * Override this method to be able to collect statistics per
	 * Contract/Service Producer
	 */
	@Override
	public MuleEvent route(MuleEvent event) throws RoutingException {

		long beforeCall = System.currentTimeMillis();
		String serviceId = event.getMessage().getProperty(VPUtil.SERVICE_NAMESPACE, PropertyScope.SESSION) + "-"
				+ event.getMessage().getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION);

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

		ExecutionTimer.start(VPUtil.TIMER_ENDPOINT);
		MuleEvent replyEvent;
		try {
			// Do the actual routing
			replyEvent = super.route(event);
		} finally {
		    long endpointTime =  ExecutionTimer.stop(VPUtil.TIMER_ENDPOINT);
		    event.getMessage().setProperty(X_SKLTP_PRODUCER_RESPONSETIME, endpointTime, PropertyScope.OUTBOUND); 
		}

		/*
		 * Restore properties
		 */
		for (final String prop : event.getMessage().getPropertyNames(PropertyScope.OUTBOUND)) {
			if (!BLOCKED_REQ_HEADERS.contains(prop)) {
				replyEvent.getMessage().setProperty((String) prop,
						event.getMessage().getProperty((String) prop, PropertyScope.OUTBOUND), PropertyScope.OUTBOUND);
			}
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

		if (logger.isDebugEnabled()) {
			logger.debug("EndpointBuilder URI: {}", recipient);
		}

		String url = (String) recipient;
		message.setProperty(VPUtil.ENDPOINT_URL, url, PropertyScope.SESSION);
		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder(url, muleContext));
		eb.setResponseTimeout(selectResponseTimeout(message));
		eb.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
		eb.setEncoding("UTF-8");
		message.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8", PropertyScope.OUTBOUND);


		if (logger.isDebugEnabled()) {
			logger.debug("Setting HTTP properties from Mule SESSION:\n{} = {}\n{} = {}\n{} = {}", new Object[] {
				X_VP_CORRELATION_ID, message.getProperty(SOITOOLKIT_CORRELATION_ID, PropertyScope.SESSION),
				X_VP_CONSUMER_ID, message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION),
				X_VP_PRODUCER_ID, message.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION)});
		}

		MessagePropertiesTransformer mt = createOutboundTransformer();
		mt.getAddProperties().put(X_VP_CORRELATION_ID,
				message.getProperty(SOITOOLKIT_CORRELATION_ID, PropertyScope.SESSION));
		mt.getAddProperties().put(X_VP_CONSUMER_ID, message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION));
		mt.getAddProperties().put(X_VP_PRODUCER_ID, message.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION));

		// FIXME:
		// This header should be removed (replaced by X_VP_PRODUCER_ID), but
		// there's a need to be backward compatible
		// because the header is actually in-use by insurance transformations.
		mt.getAddProperties().put(VPUtil.RECEIVER_ID, message.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION));

		// XXX: Make sure SOAPAction is forwarded to producer
		String action = message.getProperty("SOAPAction", PropertyScope.INBOUND);
		if (action != null) {
			mt.getAddProperties().put("SOAPAction", action);
			message.setProperty("SOAPAction", action, PropertyScope.OUTBOUND);
		}

		eb.addMessageProcessor(mt);

		Connector connector = selectConsumerConnector(url, message);
		eb.setConnector(connector);
		logger.debug("VP Consumer connector to use: {}", connector.getName());

		try {
			OutboundEndpoint ep = eb.buildOutboundEndpoint();
			logger.debug("EndpointBuilder ready!!!");
			return ep;
		} catch (InitialisationException e) {
			throw new VpTechnicalException(e);
		} catch (EndpointException e) {
			throw new VpTechnicalException(e);
		}
	}
	
	protected int selectResponseTimeout(MuleMessage message) {
		//Feature: Select response timeout provided by invoked service or use global default in responseTimeout
		int responseTimeoutValue = message.getProperty(VPUtil.FEATURE_RESPONSE_TIMOEUT, PropertyScope.INVOCATION,responseTimeout);
		logger.debug("Selected response timeout {}", responseTimeoutValue);
		return responseTimeoutValue;
	}

	private Connector selectConsumerConnector(String url, MuleMessage message) {

		boolean useKeepAlive = message.getProperty(VPUtil.FEATURE_USE_KEEP_ALIVE, PropertyScope.INVOCATION, false);

		if (url.contains("https://") && useKeepAlive) {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTPS_KEEPALIVE_NAME);
		} else if (url.contains("https://")) {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTPS_NAME);
		} else {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTP_NAME);
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
	private MessagePropertiesTransformer createOutboundTransformer() {
		logger.info("Create outbound message transformers to update/add/remove mule message properties");
		MessagePropertiesTransformer transformer = new MessagePropertiesTransformer();
		transformer.setMuleContext(muleContext);
		transformer.setOverwrite(true);
		transformer.setScope(PropertyScope.OUTBOUND);
		transformer.setAddProperties(new HashMap<String, Object>(ADD_HEADERS));
		transformer.setDeleteProperties(BLOCKED_REQ_HEADERS);
		return transformer;
	}
}
