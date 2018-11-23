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

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.logging.EventLogger;
import se.skl.tp.vp.logging.EventLoggerFactory;
import se.skl.tp.vp.logging.SessionInfo;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.VPUtil;

/**
 * Transformer that is responsible for logging exceptions using EventLogger.
 */
public class ExceptionLoggerTransformer extends AbstractMessageTransformer {
	
	private static final Logger log = LoggerFactory.getLogger(ExceptionLoggerTransformer.class);
	
	private final EventLogger<MuleMessage> eventLogger = EventLoggerFactory.createInstance();

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		log.debug("Entering ExceptionLoggerTransformer to log exception...");
		
		eventLogger.setContext(super.muleContext);	
		
		Throwable ex = message.getExceptionPayload().getException();
		
		handleException(ex, message);
		
		//logException(message.getExceptionPayload());
		return message;
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
    
	public void handleException(Throwable t, MuleMessage message) {
		startTimer();
		logException(t, message);		
		stopTimer();
	}

	private void stopTimer() {
		ExecutionTimer.stop(VPUtil.TIMER_TOTAL);
		log.info(ExecutionTimer.format());
	}

	private void startTimer() {
		Map<String, String> extraInfo = new HashMap<>();
		extraInfo.put("source", getClass().getName());
		ExecutionTimer timer = ExecutionTimer.get(VPUtil.TIMER_ENDPOINT);
		if (timer != null) {
			extraInfo.put("time.producer", String.valueOf(timer.getElapsed()));
		}
	}
    
	/**
	 * Used to log the error passed into this Exception Listener
	 * 
	 * @param t
	 *            the exception thrown
	 */
	protected void logException(Throwable t, MuleMessage message) {
		
		SessionInfo extraInfo = new SessionInfo();
		extraInfo.addSource(getClass().getName());
		extraInfo.addSessionInfo(message);
		
		MuleException muleException = ExceptionHelper.getRootMuleException(t);
		if (muleException != null) {
			addErrorMessageProperties(muleException, message);
    		eventLogger.logErrorEvent(muleException, message, null, extraInfo); 
    		
		} else {
			addErrorMessageProperties(t, message);
			eventLogger.logErrorEvent(t, "", null, extraInfo);
		}
	}

	private void addErrorMessageProperties(Throwable t, MuleMessage message) {
		message.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);
		message.setProperty(VPUtil.SESSION_ERROR_DESCRIPTION, VPUtil.nvl(t.getMessage()), PropertyScope.SESSION);
		message.setProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, VPUtil.nvl(t.toString()), PropertyScope.SESSION);
		message.setProperty(VPUtil.SESSION_HTML_STATUS, VPUtil.nvl(message.getInboundProperty("http.status")), PropertyScope.SESSION);
		String errorCode = "VP009";
		if (t instanceof VpSemanticException) {
			errorCode = ((VpSemanticException) t).getErrorCode().toString();	
		}
		else if (t.getCause() instanceof VpSemanticException) {
			errorCode = ((VpSemanticException) t.getCause()).getErrorCode().toString();
		}
		message.setProperty(VPUtil.SESSION_ERROR_CODE, errorCode, PropertyScope.SESSION);
	}

}