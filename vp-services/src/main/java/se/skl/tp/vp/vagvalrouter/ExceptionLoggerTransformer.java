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

import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;

import se.skl.tp.vp.util.EventLogger;
import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.VPUtil;

/**
 * Transformer that is responsible for logging exceptions using EventLogger.
 */
public class ExceptionLoggerTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(ExceptionLoggerTransformer.class);
	
	private final EventLogger eventLogger = new EventLogger();

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		log.debug("Entering ExceptionLoggerTransformer to log exception...");
		
		eventLogger.setMuleContext(message.getMuleContext());	
		logException(message.getExceptionPayload());
		return message;
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
    
	protected void logException(ExceptionPayload exceptionPayload) {
                
		Map<String, String> extraInfo = new HashMap<String, String>();
		extraInfo.put("source", getClass().getName());
		ExecutionTimer timer = ExecutionTimer.get(VPUtil.TIMER_ENDPOINT);
		if (timer != null) {
			extraInfo.put("time.producer", String.valueOf(timer.getElapsed()));
		}
	
		Throwable rootException = exceptionPayload.getRootException();

        if (rootException != null) {

        	if (rootException instanceof MessagingException) {
        		MessagingException me = (MessagingException)rootException;
            	
        		MuleMessage msg = me.getEvent().getMessage();
            	
        		Throwable ex = (me.getCause() == null ? me : me.getCause());
        		
        		msg.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);
        		msg.setProperty(VPUtil.SESSION_ERROR_DESCRIPTION, nvl(ex.getMessage()), PropertyScope.SESSION);
        		msg.setProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, nvl(ex.toString()), PropertyScope.SESSION);
            	msg.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);

        		eventLogger.addSessionInfo(msg, extraInfo);
        		eventLogger.logErrorEvent(ex, msg, null, extraInfo); 

        	} else {
                Map<?, ?> info = ExceptionHelper.getExceptionInfo(rootException);
        		eventLogger.logErrorEvent(rootException, info.get("Payload"), null, extraInfo);                
        	}
        	
        } else {
    		eventLogger.logErrorEvent(exceptionPayload.getException(), "", null, extraInfo);
        }
        
        // stop request.
		ExecutionTimer.stop(VPUtil.TIMER_TOTAL);
		log.info(ExecutionTimer.format());
	}
	
	static String nvl(String s) {
		return (s == null) ? "" : s;
	}
}
