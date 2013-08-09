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
package se.skl.tp.vp.vagvalrouter.producer;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.vagvalrouter.VagvalRouter;

public class VpTestProducerLogger extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(VpTestProducerLogger.class);

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		@SuppressWarnings("unchecked")
		Map<String, Object> httpHeaders = (Map<String, Object>)message.getInboundProperty("http.headers");
		
		Object orgConsumer = httpHeaders.get(VagvalRouter.X_VP_CONSUMER_ID);

		log.info("Test producer called with {}: {}", VagvalRouter.X_VP_CONSUMER_ID, orgConsumer);

		return message;
	}

}
