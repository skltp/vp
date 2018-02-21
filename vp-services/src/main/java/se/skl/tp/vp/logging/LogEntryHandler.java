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
package se.skl.tp.vp.logging;

import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_BUSINESS_CONTEXT_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_CONTRACT_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_CORRELATION_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_INTEGRATION_SCENARIO;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogLevelType;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageExceptionType;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageType;
import org.soitoolkit.commons.logentry.schema.v1.LogMetadataInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType.ExtraInfo;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType.BusinessContextId;
import org.soitoolkit.commons.mule.util.MuleUtil;
import org.soitoolkit.commons.mule.util.XmlUtil;

import se.skl.tp.vp.util.PayloadToStringTransformer;
import se.skl.tp.vp.util.VPUtil;

@SuppressWarnings("deprecation")
public class LogEntryHandler {

	private static final String UNKNOWN_MULE_CONFIGURATION = "UNKNOWN.MULE_CONFIGURATION";
	private static final String UNKNOWN_MULE_CONTEXT = "UNKNOWN.MULE_CONTEXT";

	// Logger for normal logging of code execution	
	private static final Logger log = LoggerFactory.getLogger(LogEntryHandler.class);

	protected static InetAddress HOST = null;
	protected static String HOST_NAME = "UNKNOWN";
	protected static String HOST_IP = "UNKNOWN";
	protected static String PROCESS_ID = "UNKNOWN";

	static {
		try {
			// Let's give it a try, fail silently...
			HOST       = InetAddress.getLocalHost();
			HOST_NAME  = HOST.getCanonicalHostName();
			HOST_IP    = HOST.getHostAddress();
			PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName();
		} catch (Throwable ex) {
		}
	}
	
	private String serverId;
	private String loggerName;
	// Used to transform payloads that are jaxb-objects into a xml-string
	private PayloadToStringTransformer payloadToStringTransformer;
	private MuleContext muleContext;
	private static LogEntryHandler _handler;
	
	private LogEntryHandler(MuleContext muleContext, String loggerName,
			PayloadToStringTransformer payloadToStringTransformer) {
		this.muleContext = muleContext;
		this.loggerName = loggerName;
		this.payloadToStringTransformer = payloadToStringTransformer;
	}

	public static LogEntryHandler getInstance(MuleContext muleContext, String loggerName,
			PayloadToStringTransformer payloadToStringTransformer) {
		
		if(_handler != null)
			return _handler;
		
		return new LogEntryHandler(muleContext, loggerName, payloadToStringTransformer);
	}
	
	private String getServerId() {

		if (serverId != null) return serverId;
		
		if (this.muleContext == null) return UNKNOWN_MULE_CONTEXT; 

		MuleConfiguration mConf = this.muleContext.getConfiguration();
		
		if (mConf == null) return UNKNOWN_MULE_CONFIGURATION; 
		
		return serverId = mConf.getId();
	}
	
	protected LogEvent createLogEntry(
			LogLevelType logLevel,
			MuleMessage message, 
			String logMessage,
			Map<String, String> businessContextId,
			SessionInfo extraInfo,
			Object payload,
			Throwable exception) {

			// --------------------------
			//
			// 1. Process input variables
			//
			// --------------------------

			// TODO: Will event-context always be null when an error is reported?
			// If so then its probably better to move this code to the info-logger method.
		    String           serviceImplementation = "";
			String           endpoint    = "";
	        MuleEventContext event       = RequestContext.getEventContext();
	        if (event != null) {
			    serviceImplementation   = MuleUtil.getServiceName(event);
			    URI endpointURI = event.getEndpointURI();
				endpoint                = (endpointURI == null)? "" : endpointURI.toString();
	        }
			
			String messageId             = "";
			String integrationScenarioId = ""; 
			String contractId            = ""; 
			String businessCorrelationId = "";
			String propertyBusinessContextId = null;

			if (message != null) {

				if (logLevel == LogLevelType.DEBUG) {
					@SuppressWarnings("rawtypes")
					Set names = message.getPropertyNames(PropertyScope.OUTBOUND);
					for (Object object : names) {
						Object value = message.getProperty(object.toString(), PropertyScope.OUTBOUND);
						log.debug(object + " = " + value + " (" + object.getClass().getName() + ")");
					}
				}
				
				messageId             = message.getUniqueId();
				contractId            = message.getProperty(SOITOOLKIT_CONTRACT_ID, PropertyScope.SESSION, "");
				businessCorrelationId = message.getProperty(VPUtil.SKLTP_CORRELATION_ID, PropertyScope.SESSION, "");
				integrationScenarioId = message.getProperty(SOITOOLKIT_INTEGRATION_SCENARIO, PropertyScope.SESSION, "");
				propertyBusinessContextId = message.getProperty(SOITOOLKIT_BUSINESS_CONTEXT_ID, null);
			}

			String componentId = getServerId();
			//String payloadAsString = payloadToStringTransformer.getPayloadAsString(payload);
			

		    // -------------------------
		    //
		    // 2. Create LogEvent object
		    //
		    // -------------------------
			
			// Setup basic runtime information for the log entry
			LogRuntimeInfoType lri = new LogRuntimeInfoType();
			lri.setTimestamp(XmlUtil.convertDateToXmlDate(null));
			lri.setHostName(HOST_NAME);
			lri.setHostIp(HOST_IP);
			lri.setProcessId(PROCESS_ID);
			lri.setThreadId(Thread.currentThread().getName());
			lri.setComponentId(componentId);
			lri.setMessageId(messageId);
			lri.setBusinessCorrelationId(businessCorrelationId); 
			
			// Add any business contexts
			if (businessContextId != null) {
				Set<Entry<String, String>> entries = businessContextId.entrySet();
				for (Entry<String, String> entry : entries) {
					BusinessContextId bxid = new BusinessContextId();
					bxid.setName(entry.getKey());
					bxid.setValue(entry.getValue());
					lri.getBusinessContextId().add(bxid);
				}
			}
			
			// Also add any business contexts from message properties
			if (propertyBusinessContextId != null) {
				String[] propertyArr = propertyBusinessContextId.split(",");
				
				for (String property : propertyArr) {
					String[] nameValueArr = property.split("=");
					String name = nameValueArr[0];
					String value = (nameValueArr.length > 1) ? nameValueArr[1] : "";
					BusinessContextId bxid = new BusinessContextId();
					bxid.setName(name);
					bxid.setValue(value);
					lri.getBusinessContextId().add(bxid);				
				}
				
			}
			

			// Setup basic metadata information for the log entry
			LogMetadataInfoType lmi = new LogMetadataInfoType();
			lmi.setLoggerName(loggerName);
			lmi.setIntegrationScenarioId(integrationScenarioId);
			lmi.setContractId(contractId);
			lmi.setServiceImplementation(serviceImplementation);
			lmi.setEndpoint(endpoint);

			
			// Setup basic information of the log message for the log entry
			LogMessageType lm = new LogMessageType();
			lm.setLevel(logLevel);
			lm.setMessage(logMessage);
			
			
			// Setup exception information if present
			if (exception != null) {
				LogMessageExceptionType lme = new LogMessageExceptionType();
				
				lme.setExceptionClass(exception.getClass().getName());
				lme.setExceptionMessage(exception.getMessage());
				StackTraceElement[] stArr = exception.getStackTrace();
				// we are just interested in the first lines.
				for (int i = 0; i < stArr.length && i < 10; i++) {
					lme.getStackTrace().add(stArr[i].toString());
				}		
				lm.setException(lme);
			}

			// Create the log entry object
			LogEntryType logEntry = new LogEntryType();
			logEntry.setMetadataInfo(lmi);
			logEntry.setRuntimeInfo(lri);
			logEntry.setMessageInfo(lm);
			//logEntry.setPayload(payloadAsString);

			//final String receiver = VPUtil.getReceiverId(message);
			//extraInfo.put("receiver", receiver);
			
			// Add any extra info
			if (extraInfo != null) {
				Set<Entry<String, String>> entries = extraInfo.entrySet();
				for (Entry<String, String> entry : entries) {
					ExtraInfo ei = new ExtraInfo();
					ei.setName(entry.getKey());
					ei.setValue(entry.getValue());
					logEntry.getExtraInfo().add(ei);
				}
			}
			
			// Create the final log event object
			LogEvent logEvent = new LogEvent();
			logEvent.setLogEntry(logEntry);
					
			// We are actually done :-)
			return logEvent;
		}
	
	protected void setPayload(LogEvent event, Object payload) {
		if(event != null && event.getLogEntry() != null && event.getLogEntry().getPayload() == null) {
			String payloadAsString = payloadToStringTransformer.getPayloadAsString(payload);
			event.getLogEntry().setPayload(payloadAsString);
		}
	}
}
