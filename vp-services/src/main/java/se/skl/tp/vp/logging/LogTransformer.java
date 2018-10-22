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

import static org.soitoolkit.commons.logentry.schema.v1.LogLevelType.INFO;
import static se.skl.tp.vp.logging.LogMessageEnum.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.http.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogLevelType;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.VPUtil;

/**
 * Transformer used to log messages passing a specific endpoint using the
 * event-logger Configurable properties:
 * 
 * 1. logLevel, accepts the values: FATAL, ERROR, WARNING, INFO, DEBUG and
 * TRACE. Defaults to INFO. 2. logType, any string, could be "req-in" for a
 * inbound synchronous endpoint or "msg-out" of outbound asynchronous endpoint
 * 
 * @author Magnus Larsson
 */
public class LogTransformer extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(LogTransformer.class);

	private final EventLogger<MuleMessage> eventLogger = EventLoggerFactory.createInstance();
	
	// Socket logging
	
	public void setUseSocketLogger(Boolean useSocketLogger) {
		this.eventLogger.setUseSocketLogger(useSocketLogger);
	}
	
	public void setSocketLoggerCategories(String categories) {
		this.eventLogger.setSocketLoggerCategories(categories);
	}

	public void setSocketLoggerServiceContracts(String serviceContracts) {
		this.eventLogger.setSocketLoggerServiceContracts(serviceContracts);
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
     * Set the queue name for log error messages.
     * 
     * @param queueName
     */
    public void setLogErrorQueueName(String queueName) {
        this.eventLogger.setLogErrorQueueName(queueName);
    }

    /**
     * Set the queue name for log info messages.
     * 
     * @param queueName
     */
    public void setLogInfoQueueName(String queueName) {
        this.eventLogger.setLogInfoQueueName(queueName);
    }

	//
	@Override
	public void setMuleContext(MuleContext muleContext) {
		log.debug("setMuleContext { muleContext: {} }", muleContext);
		super.setMuleContext(muleContext);
		this.eventLogger.setContext(muleContext);
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
	private String logType;

	public void setLogType(String logType) {
		this.logType = logType;
	}

	/*
	 * Property extraInfo
	 * 
	 * <custom-transformer name="logKivReqIn"
	 * class="org.soitoolkit.commons.mule.log.LogTransformer"> <spring:property
	 * name="logType" value="Received"/> <spring:property name="jaxbObjectToXml"
	 * ref="objToXml"/> <spring:property name="extraInfo"> <spring:map>
	 * <spring:entry key="id1" value="123"/> <spring:entry key="id2"
	 * value="456"/> </spring:map> </spring:property> </custom-transformer>
	 */
	private SessionInfo extraInfo;

	public void setExtraInfo(SessionInfo extraInfo) {
		this.extraInfo = extraInfo;
	}

		
	public MuleContext getMuleContext() {
		return super.muleContext;
	}
	
	/**
	 * Setter for the jaxbToXml property
	 * 
	 * @param jaxbToXml
	 */
	public void setJaxbObjectToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		this.eventLogger.setJaxbToXml(jaxbToXml);
	}

	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		
		final String logName = "log." + logType;
		ExecutionTimer.start(logName);

		try {
			
			/*
			 * Don't skip in TP
			 */
			// Skip logging if an error has occurred, then the error is logged
			// by an error handler
			// ExceptionPayload exp = message.getExceptionPayload();
			// if (exp != null) {
			// log.debug("Skip logging message, exception detected! " +
			// exp.getException().getMessage());
			// return message;
			// }

			// skip wsdl and xsd requests
			String httpQS = message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING);
			if ("wsdl".equalsIgnoreCase(httpQS) || "xsd".equalsIgnoreCase(httpQS)) {
				log.debug("Skip logging message, wsdl or xsd call detected!");
				return message;
			}

			// FIXME: Added from ST v0.4.1
			SessionInfo evaluatedExtraInfo = evaluateMapInfo(extraInfo, message);
			evaluatedExtraInfo.addSource(getClass().getName());
			
			// producer elapsed time
			ExecutionTimer timer = ExecutionTimer.get(VPUtil.TIMER_ENDPOINT);
			if (timer != null) {
				evaluatedExtraInfo.put("time.producer", String.valueOf(timer.getElapsed()));
			}
			evaluatedExtraInfo.addSessionInfo(message);

			switch (logLevel) {
			case INFO:
			case DEBUG:
			case TRACE:
				// FIXME: Changed based on ST v0.4.1. */
				// eventLogger.logInfoEvent(message, logType, null, extraInfo);
				eventLogger.logInfoEvent(message, logType, null, evaluatedExtraInfo);
				break;

			case FATAL:
			case ERROR:
			case WARNING:
				// FIXME: Changed based on ST v0.4.1. */
				// eventLogger.logErrorEvent(new RuntimeException(logType),
				// message, null, extraInfo);
				eventLogger.logErrorEvent(new RuntimeException(logType), message, null, evaluatedExtraInfo);
				break;
			}

			return message;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			ExecutionTimer.stop(logName);
			// close total timer.
			if (TYPE_XRESP_OUT.getName().equals(logType)) {
				ExecutionTimer.stop(VPUtil.TIMER_TOTAL);
				final String infoMsg = String.format("%s, %s: { %s }", 
						message.getProperty(VPUtil.SKLTP_CORRELATION_ID, PropertyScope.SESSION, ""),
						message.getProperty(VPUtil.ENDPOINT_URL, PropertyScope.SESSION, ""),
						ExecutionTimer.format());
				log.info(infoMsg);
			}
		}
	}

	// FIXME: Added from ST v0.4.1
	private SessionInfo evaluateMapInfo(SessionInfo map, MuleMessage message) {

		if (map == null)
			return new SessionInfo();

		SessionInfo evaluatedMap = new SessionInfo();
		for (Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			value = evaluateValue(key, value, message);
			evaluatedMap.put(key, value);
		}
		return evaluatedMap;
	}

	//
	
	
	private String evaluateValue(String key, String value, MuleMessage message) {
		try {
			if (muleContext.getExpressionManager().isValidExpression(value.toString())) {
				String before = value;
				@SuppressWarnings("deprecation")
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

}