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

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static se.skl.tp.vp.util.VPUtil.generateSoap11FaultWithCause;

/**
 * CheckEmptyPayloadTransformer responsible to check if return message is "" and if so replace it with a SoapFault
 * 
 */
public class CheckEmptyPayloadTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(CheckEmptyPayloadTransformer.class);
	
	private static final String nullPayload = "{NullPayload}";
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
    		
    	try {
    		String strPayload = message.getPayloadAsString();
    		
			if (strPayload.length() == 0 || strPayload.equals(nullPayload)) {
				log.debug("Found return message with length 0, replace with SoapFault because CXF doesn't like the empty string");
				String cause = "No content found! Server responded with status code: " + message.getInboundProperty("http.status");
				message.setPayload(generateSoap11FaultWithCause(cause));    	
			}
		} catch (Exception e) {
	   		log.error("Error reading message as String after check that the message is a String!.");
	   		e.printStackTrace();
		}
		return message;
    }
}
