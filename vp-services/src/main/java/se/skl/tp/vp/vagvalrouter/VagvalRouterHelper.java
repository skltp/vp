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
import java.util.List;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.VPUtil;

import org.mule.transformer.types.TypedValue;


public class VagvalRouterHelper {

    private static final Logger logger = LoggerFactory.getLogger(VagvalRouterHelper.class);
    private static final PropertyScope scope = PropertyScope.OUTBOUND;
    
	/**
	 * TP forwards properties in mule header that should not be forwarded. In
	 * the case the producer is another instance of TP (serivce platform) this
	 * can be problematic.
	 *
	 * <message-properties-transformer name="deleteMuleHeaders">
	 * <delete-message-property key="x-vp-auth-cert"/>
	 * </message-properties-transformer>
	 */
	public static void propagateDefaultProperties(MuleMessage message) {

		/**
		 * Headers to be blocked when invoking producer.
		 */
		final List<String> BLOCKED_REQ_HEADERS = Collections.unmodifiableList(Arrays.asList(new String[] {
				VPUtil.RIV_VERSION, 
				VPUtil.WSDL_NAMESPACE, 
				HttpHeaders.REVERSE_PROXY_HEADER_NAME, 
				VPUtil.SERVICECONTRACT_NAMESPACE,
				VPUtil.PEER_CERTIFICATES, "LOCAL_CERTIFICATES", 
				HttpConstants.HEADER_CONTENT_TYPE,
				"http.disable.status.code.exception.check" }));
		
		Object cookieObj = message.getInboundProperty("Set-Cookie");
		
		for(String key : BLOCKED_REQ_HEADERS) {
			message.removeProperty(key, scope);
		}
		
		message.setOutboundProperty(HttpConstants.HEADER_USER_AGENT, HttpHeaders.VP_HEADER_USER_AGENT);
		message.setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");

	}


	// XXX: Make sure SOAPAction is forwarded to producer
	public static void propagateSoapActionToProducer(MuleMessage message) {
		String action = message.getProperty("SOAPAction", PropertyScope.INBOUND);
		if (action != null) {
			message.setOutboundProperty("SOAPAction", action, DataType.STRING_DATA_TYPE);
		}
	}
	
	/*
	 * Propagate x-vp-sender-id and x-vp-instance-id from this VP instance as an outbound http property as they are both needed
	 * together for another VP to determine if x-vp-sender-id is valid to use.
	 */
	public static void propagateSenderIdAndVpInstanceIdToProducer(MuleMessage message, String url, TypedValue vpInstanceTypedValue) {
		if (!VagvalRouterHelper.isURLHTTPS(url)) {
			String senderId = message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);
			message.setOutboundProperty(HttpHeaders.X_VP_SENDER_ID, senderId, DataType.STRING_DATA_TYPE);
			message.setOutboundProperty(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceTypedValue.getValue(), vpInstanceTypedValue.getDataType());
		}
	}
	
	/*
	 * Propagate x-rivta-original-serviceconsumer-hsaid as an outbound http property.
	 */
	public static void propagateOriginalServiceConsumerHsaIdToProducer(MuleMessage message) {
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
		message.setOutboundProperty(HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, originalServiceconsumerHsaid, DataType.STRING_DATA_TYPE);
	}


	/*
	 * Propagate x-skltp-correlation-id as an outbound http property if HTTP trafic or HTTPS trafic and if property set for this!
	 */
	public static void propagateCorrelationIdToProducer(MuleMessage message, String url, Boolean propagateCorrelationIdForHttps) {
		String correlationId = message.getProperty(VPUtil.CORRELATION_ID, PropertyScope.SESSION);
				
		if (!isURLHTTPS(url)) {
			message.setOutboundProperty(HttpHeaders.X_SKLTP_CORRELATION_ID, correlationId, DataType.STRING_DATA_TYPE);						
		} else {
			if (propagateCorrelationIdForHttps) {
				message.setOutboundProperty(HttpHeaders.X_SKLTP_CORRELATION_ID, correlationId, DataType.STRING_DATA_TYPE);										
			}
		}
	}
	
	public static Boolean isURLHTTPS(String url) {
		return url.contains(VPUtil.HTTPS_PROTOCOL);
	}

}
