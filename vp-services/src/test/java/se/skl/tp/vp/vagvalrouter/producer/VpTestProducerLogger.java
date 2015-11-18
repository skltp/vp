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
package se.skl.tp.vp.vagvalrouter.producer;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.vagvalrouter.VagvalRouter;

public class VpTestProducerLogger extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(VpTestProducerLogger.class);
	
	private static String latestSenderId = null;
	private static String latestRivtaOriginalSenderId = null;
	private static String latestUserAgent = null;
	private static String latestVpInstanceId = null;
	private static String latestCorrelationId = null;

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		
		@SuppressWarnings("unchecked")
		Map<String, Object> httpHeaders = (Map<String, Object>)message.getInboundProperty("http.headers");
		
		//Sender and original sender
		String rivtaOriginalSenderId = (String)httpHeaders.get(VagvalRouter.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID);
		String vpSenderId = (String)httpHeaders.get(VagvalRouter.X_VP_SENDER_ID);
		String vpInstanceId = (String)httpHeaders.get(VagvalRouter.X_VP_INSTANCE_ID);
		
		String hostName = message.getInboundProperty("MULE_ENDPOINT");
		if (!hostName.contains(VPUtil.HTTPS_PROTOCOL)) {
			assertNotNull(vpInstanceId);
			assertNotNull(vpSenderId);
		} else {
			assertNull(vpInstanceId);
			assertNull(vpSenderId);
		}
		assertNotNull(rivtaOriginalSenderId);
		
		//Correlation id, sometimes (property) we will not send a correlation id to producer
		String vpCorrelationId = (String)httpHeaders.get(VagvalRouter.X_SKLTP_CORRELATION_ID);
	
		//Should these headers exist, they are set in se.skl.tp.vp.vagvalrouter.VagvalRouter?
		String userAgent = (String)httpHeaders.get("User-Agent");	
		assertNotNull(userAgent);
			
		log.info("Test producer called with {}: {}", VagvalRouter.X_VP_SENDER_ID, vpSenderId);
		latestSenderId = vpSenderId;
		
		log.info("Test producer called with {}: {}", VagvalRouter.X_VP_INSTANCE_ID, vpInstanceId);
		latestVpInstanceId = vpInstanceId;
	
		log.info("Test producer called with {}: {}", VagvalRouter.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, rivtaOriginalSenderId);
		latestRivtaOriginalSenderId = rivtaOriginalSenderId;

		log.info("Test producer called with {}: {}", VagvalRouter.X_SKLTP_CORRELATION_ID, vpCorrelationId);
		latestCorrelationId = vpCorrelationId;

		log.info("Test producer called with {}: {}", "User-Agent", userAgent);
		latestUserAgent = userAgent;
		
		return message;
	}
	
	public static String getLatestSenderId() {
		return latestSenderId;
	}

	public static String getLatestRivtaOriginalSenderId() {
		return latestRivtaOriginalSenderId;
	}

	public static String getLatestUserAgent() {
		return latestUserAgent;
	}

	public static String getLatestVpInstanceId() {
		return latestVpInstanceId;
	}

	public static String getLatestCorrelationId() {
		return latestCorrelationId;
	}
}
