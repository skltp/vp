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

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType.ExtraInfo;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageExceptionType;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageType;
import org.soitoolkit.commons.logentry.schema.v1.LogMetadataInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType.BusinessContextId;
import se.skl.tp.vp.util.VPUtil;


/**
 * Base class for event logging
 *
 */
public abstract class EventLoggerBase {

	// EventLogger specific logger of message execution in VP
	protected static final Logger messageLogger = LoggerFactory.getLogger("org.soitoolkit.commons.mule.messageLogger");
	protected static final Logger socketLogger = LoggerFactory.getLogger("se.skltp.mule.logging.socketLogger");

	private static final Logger logger = LoggerFactory.getLogger(EventLoggerBase.class);
			
	private static final String MSG_ID = "soi-toolkit.log";
	private static final String LOG_EVENT_INFO = "logEvent-info";
	private static final String LOG_EVENT_ERROR = "logEvent-error";
	private static final String LOG_EVENT_DEBUG = "logEvent-debug";
	private static final String LOG_STRING = MSG_ID + 
		"\n** {}.start ***********************************************************" +
		"\nIntegrationScenarioId={}\nContractId={}\nLogMessage={}\nServiceImpl={}\nHost={} ({})\nComponentId={}\nEndpoint={}\nMessageId={}\nBusinessCorrelationId={}\nBusinessContextId={}\nExtraInfo={}\nPayload={}" + 
		"{}" + // Placeholder for stack trace info if an error is logged
		"\n** {}.end *************************************************************";

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

	private Boolean useSocketLogger = false;
	public void setUseSocketLogger(Boolean useSocketLogger) {
		this.useSocketLogger = useSocketLogger == null ? false : useSocketLogger;
	}
	
	private List<String> categoriesList;
	public void setSocketLoggerCategories(String categories) {
		if(categories == null || categories.isEmpty()) {
			categoriesList = new ArrayList<String>();
		} else {
			String [] s = categories.split(",");
			this.categoriesList = Arrays.asList(s);
		}
	}

	private List<String> serviceContractList;
	public void setSocketLoggerServiceContracts(String serviceContracts) {
		if(serviceContracts == null || serviceContracts.isEmpty()) {
			serviceContractList = new ArrayList<String>();
		} else {
			String [] s = serviceContracts.split(",");
			this.serviceContractList = Arrays.asList(s);
		}
	}
	
	protected boolean socketLogging(LogEvent logEvent, SessionInfo extraInfo) {

		if(!useSocketLogger) {
			return false;
		}
		
		if(!socketLogger.isDebugEnabled()) {
			return false;
		}
		
		String logMsgType = logEvent.getLogEntry().getMessageInfo().getMessage();

		if(categoriesList != null && !categoriesList.isEmpty() && !categoriesList.contains(logMsgType)) {
			return false;
		}

		String serviceContract = extraInfo.get(VPUtil.SERVICECONTRACT_NAMESPACE);

		if(serviceContractList != null && !serviceContractList.isEmpty() && !serviceContractList.contains(serviceContract)) {
			return false;
		}
		return true;
	}

	protected void logSocketEvent(LogEvent logEvent) {
		logger.info("Socket logging for messageid {}", logEvent.getLogEntry().getRuntimeInfo().getMessageId());
		String logMsg = formatLogMessage(LOG_EVENT_DEBUG, logEvent,true);
		socketLogger.debug(logMsg);	
	}
	
	protected void logDebugEvent(LogEvent logEvent) {
		String logMsg = formatLogMessage(LOG_EVENT_DEBUG, logEvent, true);
		messageLogger.debug(logMsg);	
	}
	
	protected void logInfoEvent(LogEvent logEvent) {
		String logMsg = formatLogMessage(LOG_EVENT_INFO, logEvent);
		messageLogger.info(logMsg);
	}
	
	protected void logErrorEvent(LogEvent logEvent) {
		String logMsg = formatLogMessage(LOG_EVENT_ERROR, logEvent);
		messageLogger.error(logMsg);
	}
	
	protected String formatLogMessage(String logEventName, LogEvent logEvent) {
		return formatLogMessage(logEventName, logEvent, false);
	}

	protected String formatLogMessage(String logEventName, LogEvent logEvent, boolean logPayLoad) {
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
	
	protected String businessContextIdToString(List<BusinessContextId> businessContextIds) {
		
		if (businessContextIds == null) return "";
		
		StringBuffer businessContextIdString = new StringBuffer();
		for (BusinessContextId bci : businessContextIds) {
			businessContextIdString.append("\n-").append(bci.getName()).append("=").append(bci.getValue());
		}
		return businessContextIdString.toString();
	}

	protected String extraInfoToString(List<ExtraInfo> extraInfo) {
		
		if (extraInfo == null) return "";
		
		StringBuffer extraInfoString = new StringBuffer();
		for (ExtraInfo ei : extraInfo) {
			extraInfoString.append("\n-").append(ei.getName()).append("=").append(ei.getValue());
		}
		return extraInfoString.toString();
	}

}
