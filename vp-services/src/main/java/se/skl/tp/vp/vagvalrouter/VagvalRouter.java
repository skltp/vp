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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.CorrelationMode;
import org.mule.routing.outbound.AbstractRecipientList;
import org.mule.transformer.types.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.EventLogger;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.MessageProperties;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;

public class VagvalRouter extends AbstractRecipientList {

    private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	private int responseTimeout;

	private AddressingHelper addrHelper;

	private String vpInstanceId;
	private TypedValue vpInstanceTypedValue;
	
	private Boolean propagateCorrelationIdForHttps;
	
	private int retryRoute;
	
	private final EventLogger eventLogger = new EventLogger();

	private MessageProperties messageProperties;

	/**
	 * Set value to be used in HTTP header x-vp-instance-id.
	 *
	 * @param vpInstanceId
	 */
	public void setVpInstanceId(String vpInstanceId) {
		this.vpInstanceId = VPUtil.trimProperty(vpInstanceId);
		vpInstanceTypedValue = new TypedValue(vpInstanceId, DataType.STRING_DATA_TYPE);
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

	public void setRetryRouteAfterMs(final int retry) {
		retryRoute = retry;
	}
	
	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		setAddressingHelper(new AddressingHelper(vagvalAgent, vpInstanceId));
	}
	
	public void setDisableMuleCorrelation(boolean disabled) {
		 if (disabled) 
		 {
			 setEnableCorrelation(CorrelationMode.NEVER);
		 }
	}

	public void setMessageProperties(MessageProperties messageProperties) {
		this.messageProperties = messageProperties;
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
			replyEvent = doRoute(event);
		} catch (RoutingException re) {
			/*
			 * RoutingExceotion goes here, e.g when unable to connect to producer
			 * or timeout occurs.
			 */

			//TODO: Is it possible to get failing endpoint any other way, e.g from exception?
			String addr = addrHelper.getAddress(event.getMessage());
			String cause = messageProperties.get(VpSemanticErrorCodeEnum.VP009, addr);
			setSoapFaultInResponse(event, cause, VpSemanticErrorCodeEnum.VP009.toString());
			logException(event.getMessage(), new VpSemanticException(cause, VpSemanticErrorCodeEnum.VP009));
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

		// Set and reove properties on the mule message.
		
		VagvalRouterHelper.propagateDefaultProperties(message);
		VagvalRouterHelper.propagateSenderIdAndVpInstanceIdToProducer(message, url, vpInstanceTypedValue);
		VagvalRouterHelper.propagateOriginalServiceConsumerHsaIdToProducer(message);
		VagvalRouterHelper.propagateSoapActionToProducer(message);
		VagvalRouterHelper.propagateCorrelationIdToProducer(message,url, propagateCorrelationIdForHttps);

		//endpoint_url only set here in session to be able to log in EventLogger
		message.setProperty(VPUtil.ENDPOINT_URL, url, PropertyScope.SESSION);

		// Create EndpointBuilder
		
		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder(url, getMuleContext()));
		eb.setResponseTimeout(selectResponseTimeout(message));
		eb.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
		eb.setEncoding("UTF-8");

		Connector connector = selectConsumerConnector(url, message);
		eb.setConnector(connector);
		logger.debug("VP Consumer connector to use: {}", connector.getName());
		
		try {
			OutboundEndpoint ep = eb.buildOutboundEndpoint();
			if(null != ep)
				getMuleContext().getRegistry().applyLifecycle(ep);
			logger.debug("EndpointBuilder ready!!!");
			return ep;
		} catch (InitialisationException e) {
			throw new VpTechnicalException(e);
		} catch (EndpointException e) {
			throw new VpTechnicalException(e);
		} catch (MuleException e) {
			throw new VpTechnicalException(e);
		}
	}

	private MuleEvent doRoute(MuleEvent event) throws RoutingException {
		MuleEvent replyEvent = null;
		try {
			replyEvent = super.route(event);
		} catch(CouldNotRouteOutboundMessageException ec) {
			
			if(retryRoute == 0)
				throw ec;
			else {
				logger.error("Could not route. Will retry after {} sec ...", retryRoute);
				try {
					Thread.sleep(retryRoute);
				} catch (InterruptedException e) {
					throw ec;
				}
				replyEvent = super.route(event);
			}
		}
		return replyEvent;
	}
	
	protected int selectResponseTimeout(MuleMessage message) {
		//Feature: Select response timeout provided by invoked service or use global default in responseTimeout
		int responseTimeoutValue = message.getProperty(VPUtil.FEATURE_RESPONSE_TIMOEUT, PropertyScope.INVOCATION,responseTimeout);
		logger.debug("Selected response timeout {}", responseTimeoutValue);
		return responseTimeoutValue;
	}

	private MuleEvent setSoapFaultInResponse(MuleEvent event, String cause, String errorCode){
		VPUtil.setSoapFaultInResponse(event.getMessage(), cause, errorCode);
		return event;
	}

	private void logException(MuleMessage message, Throwable t) {
		Map<String, String> extraInfo = new HashMap<String, String>();
		extraInfo.put("source", getClass().getName());
		eventLogger.setMuleContext(getMuleContext());
		eventLogger.addSessionInfo(message, extraInfo);
		eventLogger.logErrorEvent(t, message, null, extraInfo);
	}

	private Connector selectConsumerConnector(String url, MuleMessage message) {

		boolean useKeepAlive = message.getProperty(VPUtil.FEATURE_USE_KEEP_ALIVE, PropertyScope.INVOCATION, false);

		if (VagvalRouterHelper.isURLHTTPS(url) && useKeepAlive) {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTPS_KEEPALIVE_NAME);
		} else if (VagvalRouterHelper.isURLHTTPS(url)) {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTPS_NAME);
		} else {
			return muleContext.getRegistry().lookupConnector(VPUtil.CONSUMER_CONNECTOR_HTTP_NAME);
		}
	}

}
