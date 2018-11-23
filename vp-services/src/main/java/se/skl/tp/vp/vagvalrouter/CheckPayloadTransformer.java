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

import static se.skl.tp.vp.util.VPUtil.setSoapFaultInResponse;
import static se.skl.tp.vp.util.VPUtil.getStatusMessage;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.logging.EventLogger;
import se.skl.tp.vp.logging.EventLoggerFactory;
import se.skl.tp.vp.logging.SessionInfo;
import se.skl.tp.vp.util.MessageProperties;
import se.skl.tp.vp.util.VPUtil;


/**
 * CheckEmptyPayloadTransformer responsible to check if return message is "" and if so replace it with a SoapFault
 * 
 */
public class CheckPayloadTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(CheckPayloadTransformer.class);
	private final EventLogger<MuleMessage> eventLogger = EventLoggerFactory.createInstance();
	private static final String nullPayload = "{NullPayload}";
	private static final Integer HTTP_STATUS_500 = 500;
	private static final String SOAP_XMLNS = "http://schemas.xmlsoap.org/soap/envelope/";
	private MessageProperties messageProperties;

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
    
    public void setMessageProperties(MessageProperties messageProperties) {
		this.messageProperties = messageProperties;
	}

	/**
     * Message aware transformer that checks payload
     */
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
    	// The payload should be a String as this transformer should appear after ObjetcToString transformer
    	// but below is a safeguard if this transformer is used elsewhere...
    	if (!(message.getPayload() instanceof String )) {
    		log.error("Wrong type encountered in transformer! Bailing out...");
    		throw new TransformerException(null);
		}

		Integer status = message.getOutboundProperty("http.status");
		
		String status_in = message.getInboundProperty("http.status");
		
		String addr = message.getProperty(VPUtil.ENDPOINT_URL, PropertyScope.SESSION, "<UNKNOWN>");

    	try {
    		String cause = null;
    		String strPayload = message.getPayloadAsString();
			if (strPayload == null || strPayload.length() == 0 || strPayload.equals(nullPayload)) {
				
				log.debug("Found return message with length 0, replace with SoapFault because CXF doesn't like the empty string");
				cause = messageProperties.get(VpSemanticErrorCodeEnum.VP009, addr + ". Server responded with status code: " + getStatusMessage(status_in, "NULL"));
			} else if(HTTP_STATUS_500.equals(status) && !strPayload.contains(SOAP_XMLNS)) {

				
				// See ExceptionTransformer
				// We must handle this error here, where payload is not xml. 
				// Otherwise there will be a cxf error and we will end up in the general exception handling
				
				log.debug("Found response message and http.status = 500. Response was : " + left(strPayload, 200) + "...");
				cause = messageProperties.get(VpSemanticErrorCodeEnum.VP009 , addr + ". Server responded with status code: " + getStatusMessage(status_in, "NULL"));
			}
			
			if(cause != null) {
				setSoapFaultInResponse(message, cause, VpSemanticErrorCodeEnum.VP009.toString());
				logException(message, new VpSemanticException(cause, VpSemanticErrorCodeEnum.VP009));								
			}
				
		} catch (Exception e) {
	   		log.error("An error occured in CheckPayloadTransformer!.", e);
		}
		return message;
    }
    
	private void logException(MuleMessage message, Throwable t) {
		SessionInfo extraInfo = new SessionInfo();
		extraInfo.addSessionInfo(message);
		extraInfo.addSource(getClass().getName());
		eventLogger.setContext(super.muleContext);	
		eventLogger.logErrorEvent(t, message, null, extraInfo);
	}

	private String left(String s, int len) {
		if(s == null)
			return null;
		
		int i =  s.length() > len ? len : s.length();
		return s.substring(0, i);
	}

}
