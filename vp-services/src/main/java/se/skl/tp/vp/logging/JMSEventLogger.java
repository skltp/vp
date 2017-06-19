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

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;


/**
 * Log events in a standardized way
 * 
 * @author Magnus Larsson
 *
 */
public abstract class JMSEventLogger extends EventLoggerBase {

	// Logger for normal logging of code execution	
	private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

	// Creating JaxbUtil objects (i.e. JaxbContext objects)  are costly, so we only keep one instance.
	// According to https://jaxb.dev.java.net/faq/index.html#threadSafety this should be fine since they are thread safe!
	private static final JaxbUtil JAXB_UTIL = new JaxbUtil(LogEvent.class);
	
	private String logInfoQueueName = null;
	private String logErrorQueueName = null;

	private static boolean s_enableLogToJms = true;

	/* (non-Javadoc)
	 * @see se.skl.tp.vp.logging.EventLogger#setEnableLogToJms(boolean)
	 */
	public void setEnableLogToJms(boolean enableLogToJms) {	
		s_enableLogToJms = enableLogToJms;
		
		log.debug("Logging to JMS is now {}", (s_enableLogToJms ? "enabled" : "disabled"));
		if (log.isDebugEnabled()) {
			ExceptionUtils.getFullStackTrace(new Exception());
			log.trace("- setEnableLogToJms() is called from \n{}", ExceptionUtils.getFullStackTrace(new Exception()));			
		}
	}
	
	public void setLogErrorQueueName(String logErrorQueueName) {
        this.logErrorQueueName = logErrorQueueName;
    }

	public void setLogInfoQueueName(String logInfoQueueName) {
        this.logInfoQueueName = logInfoQueueName;
    }
	
	protected void dispatchInfoEvent(LogEvent logEvent) {
		if (s_enableLogToJms) {
			String msg = JAXB_UTIL.marshal(logEvent);
			dispatchEvent(logInfoQueueName, msg);
		}
	}
	
	protected void dispatchDebugEvent(LogEvent logEvent) {
		if (s_enableLogToJms) {
			String msg = JAXB_UTIL.marshal(logEvent);
			dispatchEvent(logInfoQueueName, msg);
		}
	}

	protected void dispatchErrorEvent(LogEvent logEvent) {
		if (s_enableLogToJms) {
			String msg = JAXB_UTIL.marshal(logEvent);
			dispatchEvent(logErrorQueueName, msg);
		}
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

	protected abstract Session getSession() throws JMSException;


	/* (non-Javadoc)
	 * @see se.skl.tp.vp.logging.EventLogger#sendOneTextMessage(javax.jms.Session, java.lang.String, java.lang.String)
	 */
	private void sendOneTextMessage(Session session, String queueName, String message) {

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


}
