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
		
		/*
		 * NOTE!
		 * ExceptionTransformer in its current shape only supports the case where a service 
		 * producer returns a soap fault in a HTTP 500. In case of read timeout this is not 
		 * handled correct and is a known bug. This bug is adressed in https://skl-tp.atlassian.net/browse/SKLTP-39.
		 * 
		 * In case where service producers returns soap fault in a HTTP 200, this is treated
		 * as any OK response and no exception handling is triggered. Note that according to
		 * specification WS-I Basic Profile 1.1 a soap fault should always be returned in a
		 * HTTP 500, but some cases exists where service producers do not follow the spec.
		 */

		// Take care of any error message and send it back as a SOAP Fault!
		// Is there an exception-payload?
		ExceptionPayload ep = msg.getExceptionPayload();
		if (ep == null) {
			// No, it's no, just bail out returning what we got
			logger.debug("No error, return origin message");
			
			//if (msg.getPayload() instanceof NullPayload) {
			  //setSoapFault(msg, String.format(ERR_MSG, "response paylaod is emtpy, connection closed"), "", getEndpoint().getEndpointURI().getAddress());
			//}
			return msg;
		}
		
		 msg.setExceptionPayload(null);
		 msg.setProperty("http.status", 500, PropertyScope.OUTBOUND);
		
        return msg;
 	}

	
	//
	static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "vp-node";
		}
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
