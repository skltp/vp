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

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionTransformer extends AbstractMessageTransformer {

	private static Logger logger = LoggerFactory.getLogger(ExceptionTransformer.class);

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
			return msg;
		}
				
		msg.setExceptionPayload(null);
		msg.setProperty("http.status", 500, PropertyScope.OUTBOUND);
		
		return msg;
 	}
}
