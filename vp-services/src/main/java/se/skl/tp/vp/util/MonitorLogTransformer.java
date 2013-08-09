/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
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
package se.skl.tp.vp.util;

import static org.soitoolkit.commons.logentry.schema.v1.LogLevelType.INFO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.message.ExceptionMessage;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.http.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogLevelType;
import org.soitoolkit.commons.mule.api.log.EventLogMessage;
import org.soitoolkit.commons.mule.api.log.EventLogger;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import org.soitoolkit.commons.mule.log.DefaultEventLogger;
import org.soitoolkit.commons.mule.log.EventLoggerFactory;

/**
 * Transforms for active monitoring (PingForConfiguration).
 * 
 * @author Peter
 *
 */
public class MonitorLogTransformer extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(MonitorLogTransformer.class);

	private EventLogger eventLogger;

	private Pattern pattern;
	private String senderIdPropertyName;
	private String whiteList;
	private PayloadToStringTransformer payloadToStringTransformer;
	
	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(this.senderIdPropertyName + "=([^,]+)");
	}

	//
	public void setWhiteList(final String whiteList) {
		this.whiteList = whiteList;
	}


	@Override
	public void setMuleContext(MuleContext muleContext) {
		super.setMuleContext(muleContext);

		log.debug("MuleContext injected");

		// Also inject the muleContext in the event-logger (since we create the event-logger for now)
		if (eventLogger == null) {
			eventLogger = EventLoggerFactory.getEventLogger(muleContext);
		}
	
		// TODO: this is an ugly workaround for injecting the jaxbObjToXml dependency ...
		if (eventLogger instanceof DefaultEventLogger) {
			((DefaultEventLogger) eventLogger).setJaxbToXml(payloadToStringTransformer.getJaxbObjectToXmlTransformer());
		}
	}

	/*
	 * Property logLevel
	 */
	private LogLevelType logLevel = INFO;

	public void setLogLevel(LogLevelType logLevel) {
		this.logLevel = logLevel;
	}

	/*
	 * Property logType
	 */
	private String logType = "";

	public void setLogType(String logType) {
		this.logType = logType;
	}

	/*
	 * Property integrationScenario
	 */
	private String integrationScenario = "";

	public void setIntegrationScenario(String integrationScenario) {
		this.integrationScenario = integrationScenario;
	}

	/*
	 * Property contractId
	 */
	private String contractId = "";

	public void setContractId(String contractId) {
		this.contractId = contractId;
	}

	private Map<String, String> businessContextId;

	public void setBusinessContextId(Map<String, String> businessContextId) {
		this.businessContextId = businessContextId;
	}

	private Map<String, String> extraInfo;

	public void setExtraInfo(Map<String, String> extraInfo) {
		this.extraInfo = extraInfo;
	}


	public void setJaxbObjectToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		if (eventLogger instanceof DefaultEventLogger) {
			((DefaultEventLogger) eventLogger).setJaxbToXml(jaxbToXml);
		}
		this.payloadToStringTransformer = new PayloadToStringTransformer(jaxbToXml);
	}

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		Map<String, String> evaluatedExtraInfo  = null;
		Map<String, String> evaluatedBusinessContextId = null;
		try {
			// Skip logging if an error has occurred, then the error is logged by an error handler
			ExceptionPayload exp = message.getExceptionPayload();
			if (exp != null) {
				log.debug("Skip logging message, exception detected! " + exp.getException().getMessage());
				return message;
			}

			String httpQS = message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING);
			if ("wsdl".equalsIgnoreCase(httpQS) || "xsd".equalsIgnoreCase(httpQS)) {
				log.debug("Skip logging message, wsdl or xsd call detected!");
				return message;
			}

			evaluatedExtraInfo = evaluateMapInfo(extraInfo, message);
			evaluatedBusinessContextId = evaluateMapInfo(businessContextId, message);

			if (evaluatedExtraInfo == null) {
				evaluatedExtraInfo = new HashMap<String, String>();
			}

			String producerId = (String) message.getProperty("producerId", PropertyScope.SESSION);
			evaluatedExtraInfo.put("producerId", producerId);
			evaluatedExtraInfo.put("source", getClass().getName());

			if (message.getProperty(VPUtil.SERVICE_NAMESPACE, PropertyScope.SESSION) == null) {
				message.setProperty(VPUtil.SERVICE_NAMESPACE, "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21", PropertyScope.SESSION);
				message.setProperty(VPUtil.RIV_VERSION, "RIVTABP21", PropertyScope.SESSION);
			}
			
			if (log.isDebugEnabled()) {
				log.debug(toDebugLogString(evaluatedExtraInfo, evaluatedBusinessContextId));
			}

			switch (logLevel) {
			case INFO:
			case DEBUG:
			case TRACE:
				EventLogMessage infoMsg = new EventLogMessage();
				infoMsg.setLogMessage(logType);
				infoMsg.setMuleMessage(message);
				infoMsg.setIntegrationScenario(integrationScenario);
				infoMsg.setContractId(contractId);
				infoMsg.setBusinessContextId(evaluatedBusinessContextId);
				infoMsg.setExtraInfo(evaluatedExtraInfo);
				
				eventLogger.logInfoEvent(infoMsg);
				break;

			case FATAL:
			case ERROR:
			case WARNING:
				// eventLogger.logErrorEvent(new RuntimeException(logType), message, integrationScenario, contractId,
				// null, extraInfo);
				EventLogMessage errorMsg = new EventLogMessage();
				errorMsg.setMuleMessage(message);
				errorMsg.setIntegrationScenario(integrationScenario);
				errorMsg.setContractId(contractId);
				errorMsg.setBusinessContextId(evaluatedBusinessContextId);
				errorMsg.setExtraInfo(evaluatedExtraInfo);

				if (message.getPayload() instanceof ExceptionMessage) {
					ExceptionMessage me = (ExceptionMessage) message.getPayload();
					Throwable ex = me.getException();
					if (ex.getCause() != null) {
						ex = ex.getCause();
					}
					eventLogger.logErrorEvent(ex, errorMsg);
				} else {
					String evaluatedLogType = evaluateValue("logType", logType, message);
					eventLogger.logErrorEvent(new RuntimeException(evaluatedLogType), errorMsg);
				}
				break;
			}

		} catch (Exception e) {
			log.error(toDebugLogString(evaluatedExtraInfo, evaluatedBusinessContextId), e);
		}
		
		return message;

	}

	// returns a context log mesage
	private String toDebugLogString(Map<String, String> evaluatedExtraInfo, Map<String, String> evaluatedBusinessContextId ) {
		return String.format("LogEvent [logType: %s, intergrationScenario: %s, contractId: %s, %s, %s]",
				logType, integrationScenario, contractId,
				toString("businessContextId", evaluatedBusinessContextId),
				toString("extraInfo", evaluatedExtraInfo));
	}
	
 	// returns a log string
	private static String toString(String name, Map<String, String> map) {
		StringBuilder b = new StringBuilder();
		b.append(name);
		b.append(": [");
		if (map != null) {
			boolean d = false;
			for (Map.Entry<String, String> e : map.entrySet()) {
				if (d) {
					b.append(", ");
				} else {
					d = true;
				}
				b.append(String.format("[key: %s, value: %s]", e.getKey(), e.getValue()));
			}
		}
		b.append("]");
		return b.toString();	
	}

	//
	private Map<String, String> evaluateMapInfo(Map<String, String> map, MuleMessage message) {

		if (map == null)
			return null;

		Set<Entry<String, String>> ei = map.entrySet();
		Map<String, String> evaluatedMap = new HashMap<String, String>();
		for (Entry<String, String> entry : ei) {
			String key = entry.getKey();
			String value = entry.getValue();
			value = evaluateValue(key, value, message);
			evaluatedMap.put(key, value);
		}
		return evaluatedMap;
	}

	private String evaluateValue(String key, String value, MuleMessage message) {
		try {
			if (isValidExpression(value)) {
				String before = value;
				Object eval = muleContext.getExpressionManager().evaluate(value.toString(), message);

				if (eval == null) {
					value = "UNKNOWN";

				} else if (eval instanceof List) {
					@SuppressWarnings("rawtypes")
					List l = (List) eval;
					value = l.get(0).toString();

				} else {
					value = eval.toString();
				}
				if (log.isDebugEnabled()) {
					log.debug("Evaluated extra-info for key: " + key + ", " + before + " ==> " + value);
				}
			}
		} catch (Throwable ex) {
			String errMsg = "Faild to evaluate expression: " + key + " = " + value;
			log.warn(errMsg, ex);
			value = errMsg + ", " + ex;
		}
		return value;
	}

	//
	private boolean isValidExpression(String expression) {
		try {
			return muleContext.getExpressionManager().isValidExpression(expression);
		} catch (Throwable ex) {
			return false;
		}
	}
}
