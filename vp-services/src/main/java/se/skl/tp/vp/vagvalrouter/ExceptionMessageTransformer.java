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

import org.apache.commons.lang.StringEscapeUtils;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionMessageTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(ExceptionMessageTransformer.class);

	final static String SOAP_FAULT = 
					"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
					"  <soapenv:Header/>" + 
					"  <soapenv:Body>" + 
					"    <soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
					"      <faultcode>soap:Server</faultcode>\n" + 
					"      <faultstring>%s</faultstring>\n" +
					"    </soap:Fault>" + 
					"  </soapenv:Body>" + 
					"</soapenv:Envelope>";

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		log.debug("Entering ExceptionMessageTransformer to transform exception to correct soap fault...");

		String cause = message.getExceptionPayload().getMessage();
		message.setPayload(transformToSoapFault(cause));
		message.setProperty("http.status", 500, PropertyScope.OUTBOUND);
		return message;
	}
	
	static final String escape(final String s) {
		return StringEscapeUtils.escapeXml(s);
	}
	
	static final String transformToSoapFault(final String cause) {
		return String.format(SOAP_FAULT, escape(cause));
	}


}
