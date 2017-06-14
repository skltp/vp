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
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.VPUtil;

/**
 * UseOrCreateCorrelationIdTransformer responsible to extract correlation id or create a new one and save in a session variable.
 * 
 */
public class UseOrCreateCorrelationIdTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(UseOrCreateCorrelationIdTransformer.class);
	
    /**
     * Message aware transformer that handle correlation id 
     */
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
    		
		String correlationId = message.getProperty(HttpHeaders.X_SKLTP_CORRELATION_ID, PropertyScope.INBOUND, null);
		
		if (correlationId == null) {
			log.debug("Correlation id not found in http header create a new one!");			
			correlationId = UUID.getUUID();
		}	
		message.setProperty(VPUtil.SKLTP_CORRELATION_ID, correlationId, PropertyScope.SESSION);
          
        return message;
    }
}
