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

import java.util.Map;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.transport.jms.JmsConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogLevelType;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import org.soitoolkit.commons.mule.util.MuleUtil;
import se.skl.tp.vp.util.PayloadToStringTransformer;



/**
 * Log events in a standardized way
 * 
 * @author Magnus Larsson
 *
 */
public class MuleEventLogger extends JMSEventLogger implements EventLogger<MuleMessage> {

	// Logger for normal logging of code execution	
	private static final Logger log = LoggerFactory.getLogger(EventLogger.class);


	private MuleContext muleContext;

	@Override
	public <F> void setContext(F muleContext) {
		log.debug("setMuleContext { muleContext: {} }", muleContext);
		this.muleContext = (MuleContext)muleContext;
	}


	// Used to transform payloads that are jaxb-objects into a xml-string
	private PayloadToStringTransformer payloadToStringTransformer;

	@Override
	public void setJaxbToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		this.payloadToStringTransformer  = new PayloadToStringTransformer(jaxbToXml);
	}
	
	//
	/* (non-Javadoc)
	 * @see se.skl.tp.vp.logging.EventLogger#logInfoEvent(org.mule.api.MuleMessage, java.lang.String, java.util.Map, java.util.Map)
	 */
	@Override
	public void logInfoEvent (
		MuleMessage message,
		String      logMessage,
		Map<String, String> businessContextId,
		SessionInfo extraInfo) {
		
		LogEntryHandler handler = LogEntryHandler.getInstance(muleContext, messageLogger.getName(), payloadToStringTransformer);
		
		LogEvent logEvent = null;
		//Only log payload when DEBUG is defined in log4j.xml
		if(messageLogger.isDebugEnabled()){
			logEvent = handler.createLogEntry(LogLevelType.DEBUG, message, logMessage, businessContextId, extraInfo, message.getPayloadForLogging(), null);
			dispatchDebugEvent(logEvent);
			logDebugEvent(logEvent);
		}else if (messageLogger.isInfoEnabled()) {
			logEvent = handler.createLogEntry(LogLevelType.INFO, message, logMessage, businessContextId, extraInfo, null, null);
			dispatchInfoEvent(logEvent);
			logInfoEvent(logEvent);
		}
		if(socketLogging(logEvent, extraInfo) && logEvent != null) {
			handler.setPayload(logEvent, message.getPayloadForLogging());
			logSocketEvent(logEvent);
		}

	}

	//
	/* (non-Javadoc)
	 * @see se.skl.tp.vp.logging.EventLogger#logErrorEvent(java.lang.Throwable, org.mule.api.MuleMessage, java.util.Map, java.util.Map)
	 */
	@Override
	public void logErrorEvent (
		Throwable   error,
		MuleMessage message,
		Map<String, String> businessContextId,
		SessionInfo extraInfo) {

		LogEntryHandler handler = LogEntryHandler.getInstance(muleContext, messageLogger.getName(), payloadToStringTransformer);

		LogEvent logEvent = handler.createLogEntry(LogLevelType.ERROR, message, error.toString(), businessContextId, extraInfo, message.getPayload(), error);
		dispatchErrorEvent(logEvent);
		logErrorEvent(logEvent);
	}

	/**
	 * Use this when there is no message or payload to extract
	 */
	@Override
	public void logErrorEvent (
		Throwable   error,
		String      payload,
		Map<String, String> businessContextId,
		SessionInfo extraInfo) {

		LogEntryHandler handler = LogEntryHandler.getInstance(muleContext, messageLogger.getName(), payloadToStringTransformer);

		LogEvent logEvent = handler.createLogEntry(LogLevelType.ERROR, null, error.toString(), businessContextId, extraInfo, payload, error);
		dispatchErrorEvent(logEvent);
		logErrorEvent(logEvent);
	}

	private Session getSession(JmsConnector jmsConn) throws JMSException {
		Connection c = jmsConn.getConnection();
		Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
		return s;
	}

	@Override
	protected Session getSession() throws JMSException {
		JmsConnector jmsConn = (JmsConnector)MuleUtil.getSpringBean(this.muleContext, "soitoolkit-jms-connector");
		return getSession(jmsConn);
	}

	/*
	private String getServerId() {

		if (serverId != null) return serverId;
		
		if (this.muleContext == null) return UNKNOWN_MULE_CONTEXT; 

		MuleConfiguration mConf = this.muleContext.getConfiguration();
		
		if (mConf == null) return UNKNOWN_MULE_CONFIGURATION; 
		
		return serverId = mConf.getId();
	}
*/
	/*
	private LogEvent createLogEntry(
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
			businessCorrelationId = message.getProperty(SOITOOLKIT_CORRELATION_ID, PropertyScope.SESSION, "");
			integrationScenarioId = message.getProperty(SOITOOLKIT_INTEGRATION_SCENARIO, PropertyScope.SESSION, "");
			propertyBusinessContextId = message.getProperty(SOITOOLKIT_BUSINESS_CONTEXT_ID, null);
		}

		String componentId = getServerId();
		String payloadAsString = payloadToStringTransformer.getPayloadAsString(payload);
		

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
		lmi.setLoggerName(messageLogger.getName());
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
		logEntry.setPayload(payloadAsString);

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
	*/
}
