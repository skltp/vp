/* 
 * Licensed to the soi-toolkit project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The soi-toolkit project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.skl.tp.vp.util;

import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_BUSINESS_CONTEXT_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_CONTRACT_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_CORRELATION_ID;
import static org.soitoolkit.commons.mule.core.PropertyNames.SOITOOLKIT_INTEGRATION_SCENARIO;
import static se.skl.tp.vp.util.VPUtil.nvl;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.jms.JmsConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType.ExtraInfo;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogLevelType;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageExceptionType;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageType;
import org.soitoolkit.commons.logentry.schema.v1.LogMetadataInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType.BusinessContextId;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;
import org.soitoolkit.commons.mule.util.MuleUtil;
import org.soitoolkit.commons.mule.util.XmlUtil;

/**
 * Log events in a standardized way
 * 
 * @author Magnus Larsson
 *
 */
public class EventLogger {

	private static final Logger messageLogger = LoggerFactory.getLogger("org.soitoolkit.commons.mule.messageLogger");

	private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

	// Creating JaxbUtil objects (i.e. JaxbContext objects)  are costly, so we only keep one instance.
	// According to https://jaxb.dev.java.net/faq/index.html#threadSafety this should be fine since they are thread safe!
	private static final JaxbUtil JAXB_UTIL = new JaxbUtil(LogEvent.class);

	private static final String MSG_ID = "soi-toolkit.log";
	private static final String LOG_EVENT_INFO = "logEvent-info";
	private static final String LOG_EVENT_ERROR = "logEvent-error";
	private static final String LOG_STRING = MSG_ID + 
		"\n** {}.start ***********************************************************" +
		"\nIntegrationScenarioId={}\nContractId={}\nLogMessage={}\nServiceImpl={}\nHost={} ({})\nComponentId={}\nEndpoint={}\nMessageId={}\nBusinessCorrelationId={}\nBusinessContextId={}\nExtraInfo={}\nPayload={}" + 
		"{}" + // Placeholder for stack trace info if an error is logged
		"\n** {}.end *************************************************************";

	private static InetAddress HOST = null;
	private static String HOST_NAME = "UNKNOWN";
	private static String HOST_IP = "UNKNOWN";
	private static String PROCESS_ID = "UNKNOWN";

	private String serverId = null; // Can't read this one at class initialization because it is not set at that time. Can also be different for different loggers in the same JVM (e.g. multiple wars in one servlet container with shared classes?))
	private MuleContext muleContext;
	
	// Used to transform payloads that are jaxb-objects into a xml-string
	private PayloadToStringTransformer payloadToStringTransformer;


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

	public EventLogger() {		
	}
	
	//
	public EventLogger(MuleContext muleContext) {
		setMuleContext(muleContext);
	}
	
	
	public void setMuleContext(MuleContext muleContext) {
		log.debug("setMuleContext { muleContext: {} }", muleContext);
		this.muleContext = muleContext;
	}
	
	
	/**
	 * Setter for the jaxbToXml property
	 * 
	 * @param jaxbToXml
	 */
	public void setJaxbToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		this.payloadToStringTransformer  = new PayloadToStringTransformer(jaxbToXml);
	}

	//
	public void logInfoEvent (
		MuleMessage message,
		String      logMessage,
		Map<String, String> businessContextId,
		Map<String, String> extraInfo) {
		
		if (messageLogger.isInfoEnabled()) {
			LogEvent logEvent = createLogEntry(LogLevelType.INFO, message, logMessage, businessContextId, extraInfo, message.getPayload(), null);
			String xmlString = JAXB_UTIL.marshal(logEvent);
			dispatchInfoEvent(xmlString);

			String logMsg = formatLogMessage(LOG_EVENT_INFO, logEvent);
			messageLogger.info(logMsg);
		}
	}

	//
	public void logErrorEvent (
		Throwable   error,
		MuleMessage message,
		Map<String, String> businessContextId,
		Map<String, String> extraInfo) {

		LogEvent logEvent = createLogEntry(LogLevelType.ERROR, message, error.toString(), businessContextId, extraInfo, message.getPayload(), error);
		
		String logMsg = formatLogMessage(LOG_EVENT_ERROR, logEvent);
		messageLogger.error(logMsg);

		String xmlString = JAXB_UTIL.marshal(logEvent);
		dispatchErrorEvent(xmlString);
	}

	//
	public void logErrorEvent (
		Throwable   error,
		Object      payload,
		Map<String, String> businessContextId,
		Map<String, String> extraInfo) {

		LogEvent logEvent = createLogEntry(LogLevelType.ERROR, null, error.toString(), businessContextId, extraInfo, payload, error);

		String logMsg = formatLogMessage(LOG_EVENT_ERROR, logEvent);
		messageLogger.error(logMsg);

		String xmlString = JAXB_UTIL.marshal(logEvent);
		dispatchErrorEvent(xmlString);
	}

	//----------------
	
	private void dispatchInfoEvent(String msg) {
		dispatchEvent("SOITOOLKIT.LOG.STORE", msg);
	}

	private void dispatchErrorEvent(String msg) {
		dispatchEvent("SOITOOLKIT.LOG.ERROR", msg);
	}

	private void dispatchEvent(String queue, String msg) {
		try {

			Session s = null;
			try {
				s = getSession();
				sendOneTextMessage(s, queue, msg);
			} finally {
	    		if (s != null) s.close(); 
			}
			
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
	}

	private Session getSession() throws JMSException {
//		JmsConnector jmsConn = (JmsConnector)MuleServer.getMuleContext().getRegistry().lookupConnector("soitoolkit-jms-connector");
		JmsConnector jmsConn = (JmsConnector)MuleUtil.getSpringBean(this.muleContext, "soitoolkit-jms-connector");
		Connection c = jmsConn.getConnection();
		Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
		return s;
	}

	public void sendOneTextMessage(Session session, String queueName, String message) {

        MessageProducer publisher = null;

	    try {
	    	publisher = session.createProducer(session.createQueue(queueName));
	        TextMessage textMessage = session.createTextMessage(message);  
	        publisher.send(textMessage);   
	
	    } catch (JMSException e) {
	        throw new RuntimeException(e);
	    } finally {
	    	try {
	    		if (publisher != null) publisher.close(); 
	    	} catch (JMSException e) {}
	    }
	}

	private String formatLogMessage(String logEventName, LogEvent logEvent) {
		LogMessageType      messageInfo  = logEvent.getLogEntry().getMessageInfo();
		LogMetadataInfoType metadataInfo = logEvent.getLogEntry().getMetadataInfo();
		LogRuntimeInfoType  runtimeInfo  = logEvent.getLogEntry().getRuntimeInfo();

		String integrationScenarioId   = metadataInfo.getIntegrationScenarioId();
		String contractId              = metadataInfo.getContractId();
		String logMessage              = messageInfo.getMessage();
		String serviceImplementation   = metadataInfo.getServiceImplementation();
		String componentId             = runtimeInfo.getComponentId();
		String endpoint                = metadataInfo.getEndpoint();
		String messageId               = runtimeInfo.getMessageId();
		String businessCorrelationId   = runtimeInfo.getBusinessCorrelationId();
		String payload                 = logEvent.getLogEntry().getPayload();
		String businessContextIdString = businessContextIdToString(runtimeInfo.getBusinessContextId());
		String extraInfoString         = extraInfoToString(logEvent.getLogEntry().getExtraInfo());
		
		StringBuffer stackTrace = new StringBuffer();
		LogMessageExceptionType lmeException = logEvent.getLogEntry().getMessageInfo().getException();
		if (lmeException != null) {
			String ex = lmeException.getExceptionClass();
			String msg = lmeException.getExceptionMessage();
			List<String> st = lmeException.getStackTrace();

			stackTrace.append('\n').append("Stacktrace=").append(ex).append(": ").append(msg);
			for (String stLine : st) {
				stackTrace.append('\n').append("\t at ").append(stLine);
			}
		}
		return MessageFormatter.arrayFormat(LOG_STRING, new String[] {logEventName, integrationScenarioId, contractId, logMessage, serviceImplementation, HOST_NAME, HOST_IP, componentId, endpoint, messageId, businessCorrelationId, businessContextIdString, extraInfoString, payload, stackTrace.toString(), logEventName}).getMessage();
	}
	
	private String businessContextIdToString(List<BusinessContextId> businessContextIds) {
		
		if (businessContextIds == null) return "";
		
		StringBuffer businessContextIdString = new StringBuffer();
		for (BusinessContextId bci : businessContextIds) {
			businessContextIdString.append("\n-").append(bci.getName()).append("=").append(bci.getValue());
		}
		return businessContextIdString.toString();
	}

	private String extraInfoToString(List<ExtraInfo> extraInfo) {
		
		if (extraInfo == null) return "";
		
		StringBuffer extraInfoString = new StringBuffer();
		for (ExtraInfo ei : extraInfo) {
			extraInfoString.append("\n-").append(ei.getName()).append("=").append(ei.getValue());
		}
		return extraInfoString.toString();
	}

	private String getServerId() {

		if (serverId != null) return serverId;
		
		if (this.muleContext == null) return "UNKNOWN.MULE_CONTEXT"; 

		MuleConfiguration mConf = this.muleContext.getConfiguration();
		
		if (mConf == null) return "UNKNOWN.MULE_CONFIGURATION"; 
		
		return serverId = mConf.getId();
	}

	private LogEvent createLogEntry(
		LogLevelType logLevel,
		MuleMessage message, 
		String logMessage,
		Map<String, String> businessContextId,
		Map<String, String> extraInfo,
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

			if (log.isDebugEnabled()) {
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
	
	//
	public void addSessionInfo(MuleMessage message, Map<String, String> map) {
		map.put(VPUtil.SENDER_ID, (String) message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION));
		map.put(VPUtil.RECEIVER_ID, (String) message.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION));
		map.put(VPUtil.RIV_VERSION, (String) message.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION));
		map.put(VPUtil.SERVICE_NAMESPACE, (String) message.getProperty(VPUtil.SERVICE_NAMESPACE, PropertyScope.SESSION));
		String endpoint = message.getProperty(VPUtil.ENDPOINT_URL, PropertyScope.SESSION);
		if (endpoint != null) {
			map.put(VPUtil.ENDPOINT_URL, endpoint);
		}
		final Boolean error = (Boolean) message.getProperty(VPUtil.SESSION_ERROR, PropertyScope.SESSION);
		if (Boolean.TRUE.equals(error)) {
			map.put(VPUtil.SESSION_ERROR, error.toString());
			map.put(VPUtil.SESSION_ERROR_DESCRIPTION,
					nvl((String) message.getProperty(VPUtil.SESSION_ERROR_DESCRIPTION, PropertyScope.SESSION)));
			map.put(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION,
					nvl((String) message.getProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, PropertyScope.SESSION)));
		}
	}
}
