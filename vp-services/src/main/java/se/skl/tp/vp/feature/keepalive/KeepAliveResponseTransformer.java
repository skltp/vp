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
package se.skl.tp.vp.feature.keepalive;

import org.apache.commons.httpclient.Header;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;

/**
 * This transformer ensures that a Connection: close is sent if the property on the endpoint featureUseKeepAlive is set to false.
 * 
 * <p>Usage:</p>
 * {@code
 * 	 <custom-transformer name="keepAlive" class="se.skl.tp.vp.feature.keepalive.KeepAliveResponseTransformer"/>
 *   <http:inbound-endpoint
 *      ...
 *    	responseTransformer-refs="keepAlive"
 *      ...
 *   />
 *   	<properties>
 *      	<spring:entry key="featureUseKeepAlive" value="false"/>
 *   	</properties>
 *   </http:inbound-endpoint>
 *   	
 * } 
 * @author par.wenaker@callistaenterprise.se
 */
public class KeepAliveResponseTransformer extends MuleMessageToHttpResponse {

	private static Logger logger = LoggerFactory.getLogger(KeepAliveResponseTransformer.class);
	
	@Override
	public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException {
		HttpResponse response = (HttpResponse) super.transformMessage(msg, outputEncoding);
				
		logger.debug("Endpoint: {}, configured keep-alive: {}", getEndpoint().getName(), featureUseKeepAlive());
		
		if(!featureUseKeepAlive()) {
			response.setKeepAlive(false);
			response.setHeader(new Header(HttpConstants.HEADER_CONNECTION, "close"));
		} 
		return response;
	}
	
	private boolean featureUseKeepAlive() {
		return Boolean.parseBoolean((String)getEndpoint().getProperty(VPUtil.FEATURE_USE_KEEP_ALIVE));	
	}
	
}
