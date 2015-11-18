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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.CorrelationMode;
import org.mule.routing.outbound.AbstractRecipientList;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transport.http.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;

import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.EventLogger;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;

public class VagvalRouter extends AbstractRecipientList {

    private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	private int responseTimeout;

	private AddressingHelper addrHelper;

	private String vpInstanceId;
	
	private Boolean propagateCorrelationIdForHttps;
	
	private final EventLogger eventLogger = new EventLogger();

	/**
	 * Set value to be used in HTTP header x-vp-instance-id.
	 *
	 * @param vpInstanceId
	 */
	public void setVpInstanceId(String vpInstanceId) {
		this.vpInstanceId = vpInstanceId;
	}

	/**
	 * Headers to be blocked when invoking producer.
	 */
	private static final List<String> BLOCKED_REQ_HEADERS = Collections.unmodifiableList(Arrays.asList(new String[] {
			VPUtil.RIV_VERSION, VPUtil.WSDL_NAMESPACE, HttpHeaders.REVERSE_PROXY_HEADER_NAME, VPUtil.SERVICECONTRACT_NAMESPACE,
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

	// for unit-testing only
	void setAddressingHelper(AddressingHelper helper) {
		this.addrHelper = helper;
	}

	public void setResponseTimeout(final int responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public void setPropagateCorrelationIdForHttps(final Boolean propagateCorrelationIdForHttps) {
		this.propagateCorrelationIdForHttps = propagateCorrelationIdForHttps;
	}

	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		setAddressingHelper(new AddressingHelper(vagvalAgent));
	}
	
	public void setDisableMuleCorrelation(boolean disabled) {
		 if (disabled) 
		 {
			 setEnableCorrelation(CorrelationMode.NEVER);
		 }
	}

	/**
	 * Enable logging to JMS, it true by default
	 *
	 * @param logEnableToJms
	 */
	public void setEnableLogToJms(boolean logEnableToJms) {
		this.eventLogger.setEnableLogToJms(logEnableToJms);
	}

	/**
	 * Setter for the jaxbToXml property
	 *
	 * @param jaxbToXml
	 */
	public void setJaxbObjectToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		this.eventLogger.setJaxbToXml(jaxbToXml);
	}

    /**
     * Set the queue name for log error messages.
     *
     * @param queueName
     */
    public void setLogErrorQueueName(String queueName) {
        this.eventLogger.setLogErrorQueueName(queueName);
    }

	@Override
	protected List<Object> getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException {
		ExecutionTimer.start(VPUtil.TIMER_ROUTE);
		try {
			String addr = addrHelper.getAddress(event.getMessage());

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

		/*
		 * VpSemanticException stored in INVOCATION scoped property 'VpSemanticException' goes here...
		 */
		VpSemanticException vpSemanticException = (VpSemanticException)event.getMessage().getInvocationProperty(VPUtil.VP_SEMANTIC_EXCEPTION);
		if (vpSemanticException != null) {
			setSoapFaultInResponse(event, vpSemanticException.getMessage(), vpSemanticException.getErrorCode().toString());
			logException(event.getMessage(), vpSemanticException);
			return event;
		}

		ExecutionTimer.start(VPUtil.TIMER_ENDPOINT);
		MuleEvent replyEvent = null;
		try {
			setDisableMuleCorrelation(true);
			// Do the actual routing
			replyEvent = super.route(event);
		} catch (RoutingException re) {
			/*
			 * RoutingExceotion goes here, e.g when unable to connect to producer
			 * or timeout occurs.
			 */

			//TODO: Is it possible to get failing endpoint any other way, e.g from exception?
			String addr = addrHelper.getAddress(event.getMessage());
			String cause = VpSemanticErrorCodeEnum.VP009 + " Error connecting to service producer at adress " + addr;
			setSoapFaultInResponse(event, cause, VpSemanticErrorCodeEnum.VP009.toString());
			logException(event.getMessage(), re);
			return event;

		} catch (RuntimeException re) {
			/*
			 * VpSemanticException goes here...
			 */
			String errorCode = "";
			if (re instanceof VpSemanticException) {
				errorCode = ((VpSemanticException) re).getErrorCode().toString();
			}
			setSoapFaultInResponse(event, re.getMessage(), errorCode);
			logException(event.getMessage(), re);
			return event;

		} finally {
		    if(replyEvent != null){
		    	long endpointTime =  ExecutionTimer.stop(VPUtil.TIMER_ENDPOINT);
		    	replyEvent.getMessage().setProperty(HttpHeaders.X_SKLTP_PRODUCER_RESPONSETIME, endpointTime, PropertyScope.OUTBOUND);
		    }
		}

		return replyEvent;
	}

	@Override
	protected OutboundEndpoint getRecipientEndpoint(MuleMessage message, Object recipient) throws RoutingException {

		if (logger.isDebugEnabled()) {
			logger.debug("EndpointBuilder URI: {}", recipient);
		}

		String url = (String) recipient;

		//endpoint_url only set here in session to be able to log in EventLogger
		message.setProperty(VPUtil.ENDPOINT_URL, url, PropertyScope.SESSION);

		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder(url, muleContext));
		eb.setResponseTimeout(selectResponseTimeout(message));
		eb.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
		eb.setEncoding("UTF-8");
		message.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8", PropertyScope.OUTBOUND);

		MessagePropertiesTransformer mt = createOutboundTransformer();
		
		if (!isURLHTTPS(url)) {
			propagateSenderIdAndVpInstanceIdToProducer(message, mt);
		}
		propagateOriginalServiceConsumerHsaIdToProducer(message, mt);
		propagateSoapActionToProducer(message, mt);
		propagateCorrelationIdToProducer(message, mt, url);
		
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

	// XXX: Make sure SOAPAction is forwarded to producer
	private void propagateSoapActionToProducer(MuleMessage message, MessagePropertiesTransformer mt) {
		String action = message.getProperty("SOAPAction", PropertyScope.INBOUND);
		if (action != null) {
			mt.getAddProperties().put("SOAPAction", action);
		}
	}

	/*
	 * Propagate x-vp-sender-id and x-vp-instance-id from this VP instance as an outbound http property as they are both needed
	 * togehter for another VP to determine if x-vp-sender-id is valid to use.
	 */
	private void propagateSenderIdAndVpInstanceIdToProducer(MuleMessage message, MessagePropertiesTransformer mt) {
		String senderId = message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);
		mt.getAddProperties().put(HttpHeaders.X_VP_SENDER_ID, senderId);
		mt.getAddProperties().put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
	}

	/*
	 * Propagate x-rivta-original-serviceconsumer-hsaid as an outbound http property.
	 */
	private void propagateOriginalServiceConsumerHsaIdToProducer(MuleMessage message,MessagePropertiesTransformer mt) {
		String senderId = message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);

		logger.debug("Exists original sender hsa id as inbound property {}?", VPUtil.ORIGINAL_SERVICE_CONSUMER_HSA_ID);
		String originalServiceconsumerHsaid = message.getProperty(VPUtil.ORIGINAL_SERVICE_CONSUMER_HSA_ID, PropertyScope.SESSION);

		if(originalServiceconsumerHsaid == null){
			logger.debug("No, original sender hsa id does not exist, instead set original sender hsa id = sender id: {}", senderId);
			originalServiceconsumerHsaid = senderId;
			//Put in session scope to be able to log in EventLogger.
			message.setProperty(VPUtil.ORIGINAL_SERVICE_CONSUMER_HSA_ID, originalServiceconsumerHsaid, PropertyScope.SESSION);
		}

		//Propagate the http header to producers
		mt.getAddProperties().put(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, originalServiceconsumerHsaid);
	}

	/*
	 * Propagate x-skltp-correlation-id as an outbound http property if HTTP trafic or HTTPS trafic and if property set for this!
	 */
	private void propagateCorrelationIdToProducer(MuleMessage message, MessagePropertiesTransformer mt, String url) {
		String correlationId = message.getProperty(VPUtil.CORRELATION_ID, PropertyScope.SESSION);
				
		if (!isURLHTTPS(url)) {
			mt.getAddProperties().put(HttpHeaders.X_SKLTP_CORRELATION_ID, correlationId);						
		} else {
			if (propagateCorrelationIdForHttps) {
				mt.getAddProperties().put(HttpHeaders.X_SKLTP_CORRELATION_ID, correlationId);										
			}
		}
	}

	protected int selectResponseTimeout(MuleMessage message) {
		//Feature: Select response timeout provided by invoked service or use global default in responseTimeout
		int responseTimeoutValue = message.getProperty(VPUtil.FEATURE_RESPONSE_TIMOEUT, PropertyScope.INVOCATION,responseTimeout);
		logger.debug("Selected response timeout {}", responseTimeoutValue);
		return responseTimeoutValue;
	}

	private MuleEvent setSoapFaultInResponse(MuleEvent event, String cause, String errorCode){
		String soapFault = VPUtil.generateSoap11FaultWithCause(cause);
		event.getMessage().setPayload(soapFault);
		event.getMessage().setExceptionPayload(null);
		event.getMessage().setProperty("http.status", 500, PropertyScope.OUTBOUND);
		event.getMessage().setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);
		event.getMessage().setProperty(VPUtil.SESSION_ERROR_CODE, errorCode, PropertyScope.SESSION);
		return event;
	}

	private void logException(MuleMessage message, Throwable t) {
		Map<String, String> extraInfo = new HashMap<String, String>();
		extraInfo.put("source", getClass().getName());
		eventLogger.setMuleContext(message.getMuleContext());
		eventLogger.addSessionInfo(message, extraInfo);
		eventLogger.logErrorEvent(t, message, null, extraInfo);
	}

	private Connector selectConsumerConnector(String url, MuleMessage message) {

		boolean useKeepAlive = message.getProperty(VPUtil.FEATURE_USE_KEEP_ALIVE, PropertyScope.INVOCATION, false);

		if (isURLHTTPS(url) && useKeepAlive) {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTPS_KEEPALIVE_NAME);
		} else if (isURLHTTPS(url)) {
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
	
	private Boolean isURLHTTPS(String url) {
		if (url.contains(VPUtil.HTTPS_PROTOCOL)) {
			return true;
		} else {
			return false;
		}
	}
}
