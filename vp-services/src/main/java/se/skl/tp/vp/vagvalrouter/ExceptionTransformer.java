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

import java.net.InetAddress;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringEscapeUtils;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.NullPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.VPUtil;

public class ExceptionTransformer extends AbstractMessageTransformer {

	private static Logger logger = LoggerFactory.getLogger(ExceptionTransformer.class);

	static String HOSTNAME = getHostname();
		
	private static final String ERR_MSG = "VP009 Exception when calling the service producer (Cause: %s)";
	
	static String SOAP_FAULT_V11 = 
			"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
					"  <soapenv:Header/>" + 
					"  <soapenv:Body>" + 
					"    <soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
					"      <faultcode>soap:Server</faultcode>\n" + 
					"      <faultstring>%s</faultstring>\n" +
					"      <faultactor>%s</faultactor>\n" +
					"      <detail>\n" +
					"        %s\n" +
					"      </detail>\n" + 
					"    </soap:Fault>" + 
					"  </soapenv:Body>" + 
					"</soapenv:Envelope>";

	public ExceptionTransformer()  {}

	@Override
	public Object transformMessage(MuleMessage msg, String encoding) throws TransformerException {
		
		logger.debug("transformMessage() called");
		
		final ExceptionPayload ep = msg.getExceptionPayload();

		if (ep == null) {
        	logger.debug("No error, return origin message");
        	if (msg.getPayload() instanceof NullPayload) {
    			setSoapFault(msg, String.format(ERR_MSG, "response paylaod is emtpy, connection closed"), "", getEndpoint().getEndpointURI().getAddress());
        	}
        	return msg;
        }
        
        final Throwable exception = ep.getException();
        logger.debug("exception: {}", exception.getMessage());
        
        final Throwable rootCause = getRootCause(exception);
        logger.debug("root cause: {}", rootCause.getMessage());
        
        if (exception instanceof RoutingException) {
        	logger.debug("routing exception");
        	String details = getDetails(msg);
    		logger.debug("details: {}", details);
			return setSoapFault(msg, String.format(ERR_MSG, rootCause.getMessage()), details, getEndpoint().getEndpointURI().getAddress());
        } else {
        	logger.debug("other exception");
        	return setSoapFault(msg, rootCause.getMessage(), "", HOSTNAME);
        }
 	}

	
	//
	static String getDetails(MuleMessage msg) {
		Object payload = msg.getOriginalPayload();
		logger.debug("payload is of type: {}", payload.getClass());
		if (payload instanceof XMLStreamReader) {
			try {
				return extractDetails((XMLStreamReader) payload);
			} catch (XMLStreamException ex) {}
		}
		return "";
	}
	
	/**
	 * Returns the SOAPFault element faultstring of the original message or an empty string of none exists.
	 * 
	 * @param reader the reader.
	 * @return the fault string or an empty string if none found.
	 * @throws XMLStreamException on errors.
	 */
	static String extractDetails(XMLStreamReader reader) throws XMLStreamException {
		int event = reader.getEventType();
		boolean fault = false;
		while (reader.hasNext()) {
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				fault = "faultstring".equals(reader.getLocalName());
				break;
			case XMLStreamConstants.CHARACTERS:
				if (fault) {
					return reader.getText();
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				fault = false;
				break;
			default:
				break;
			}
			event = reader.next();
		}
		return "";
	}
	
	//
	static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "vp-node";
		}
	}

	
	//
	static Throwable getRootCause(Throwable throwable) {
		Throwable rootCause = null;
		for (Throwable cause = throwable; cause != null; cause = cause.getCause())  {
			rootCause = cause;
			if (isVpException(rootCause)) {
				break;
			}
		}
		return rootCause;
	}

	//
	static boolean isVpException(Throwable throwable) {
		return (throwable instanceof VpSemanticException) || (throwable instanceof VpTechnicalException);
	}
	
	static String escape(String s) {
		return StringEscapeUtils.escapeXml(s);
	}
	
	private MuleMessage setSoapFault(MuleMessage msg, String cause, String details, String actor) {
		msg.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);
		msg.setProperty(VPUtil.SESSION_ERROR_DESCRIPTION, cause, PropertyScope.SESSION);
		msg.setProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, details, PropertyScope.SESSION);

		String fault = String.format(SOAP_FAULT_V11, escape(cause), escape(actor), escape(details));
		msg.setPayload(fault);
		msg.setExceptionPayload(null);
	    msg.setProperty("http.status", 500, PropertyScope.OUTBOUND);
	    
		return msg;
	}
	
}
