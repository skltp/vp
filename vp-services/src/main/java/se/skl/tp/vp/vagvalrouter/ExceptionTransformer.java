/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalrouter;

import org.apache.commons.httpclient.HttpException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.ServiceException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;

public class ExceptionTransformer extends AbstractMessageAwareTransformer {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ExceptionTransformer()  {
		registerSourceType(Object.class);
		setReturnClass(Object.class);
	}

	@Override
	public Object transform(MuleMessage msg, String encoding) throws TransformerException {
		// Check if any error
		if (msg.getExceptionPayload() != null) {
			logger.debug("Exception payload detected!");
			if (msg.getExceptionPayload().getException() instanceof ServiceException ||
				msg.getExceptionPayload().getException() instanceof HttpException) {
				
				// Check for defined TP exceptions
				if (msg.getExceptionPayload().getException() instanceof ServiceException &&
					msg.getExceptionPayload().getException().getCause() != null ) {
					if (msg.getExceptionPayload().getException().getCause() instanceof VpSemanticException || 
						msg.getExceptionPayload().getException().getCause() instanceof VpTechnicalException) {
						return createSoapFault(msg, msg.getExceptionPayload().getException().getCause().getMessage());
					}					
				}
				// Check if we got any payload if so return it!
				if (msg.getPayload() instanceof org.mule.transport.NullPayload) {
					logger.debug("Nullpayload detected!");
					return createSoapFault(msg, "VP009 Exception when calling the service producer, connection closed");					
				} else {
					logger.debug("Payload detected!");
					msg.setExceptionPayload(null);
					return msg.getPayload();	
				}								
			} else if (msg.getExceptionPayload().getException() instanceof RoutingException) {
				// Here we could get some data in payload but don't use it!
				logger.debug("Routingexception detected!");
				return createSoapFault(msg, "VP009 Exception when calling the service producer!");					
			}
			// No defined exception above or TP exception found
			return createSoapFault(msg, "VP009 Exception when calling the service producer!");
		} else if (msg.getPayload() instanceof org.mule.transport.NullPayload) {
			// No exception pauload and no message payload
			logger.debug("Nullpayload detected in message!");
			return createSoapFault(msg, "VP009 Exception when calling the service producer, connection closed");
		}
		
		// No error, return incoming payload!
		return msg.getPayload();
	}

	private Object createSoapFault(MuleMessage msg, String srcCause) {
		StringBuffer result = new StringBuffer();
		
		createSoapFault(result, srcCause);
				
		// Now wrap it into a soap-envelope
		result = createSoapEnvelope(result);

		// Clear out exception payload as we only return created SoapFaults when exception occurs
		msg.setExceptionPayload(null);
		
		// Tell Mule that we have returned a SoapFault
		logger.debug("SoapFault returned: " + result);
						
		// Set payload explict ??
		msg.setPayload(result.toString());
		
		// Done, return the string
		return result.toString();
	}

	private void createSoapFault(StringBuffer result, String faultString) {
		result.append("<soap:Fault>");
		result.append("<faultcode>soap:Server</faultcode>");
		result.append("<faultstring>" + faultString + "</faultstring>");
		result.append("</soap:Fault>");
	}
	
	private StringBuffer createSoapEnvelope(StringBuffer result) {
		StringBuffer envelope = new StringBuffer(); 
		envelope.append("<?xml version='1.0' encoding='UTF-8'?>");
		envelope.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		envelope.append("<soap:Body>");
		envelope.append(result);
		envelope.append("</soap:Body>");
		envelope.append("</soap:Envelope>");
		return envelope;
	}
}
