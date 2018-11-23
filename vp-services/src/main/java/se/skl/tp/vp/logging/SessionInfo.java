package se.skl.tp.vp.logging;

import java.util.HashMap;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vp.util.VPUtil;

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
public class SessionInfo extends HashMap<String, String> {

	private static final long serialVersionUID = -2500406010556069761L;

	public void addSessionInfo(MuleMessage message) {
		this.put(VPUtil.SENDER_ID, message.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION));
		this.put(VPUtil.RECEIVER_ID, (String) message.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION));
		this.put(VPUtil.ORIGINAL_SERVICE_CONSUMER_HSA_ID, (String) message.getProperty(VPUtil.ORIGINAL_SERVICE_CONSUMER_HSA_ID, PropertyScope.SESSION));
		this.put(VPUtil.RIV_VERSION, (String) message.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION));
		this.put(VPUtil.WSDL_NAMESPACE, (String) message.getProperty(VPUtil.WSDL_NAMESPACE, PropertyScope.SESSION));
		this.put(VPUtil.SERVICECONTRACT_NAMESPACE, (String) message.getProperty(VPUtil.SERVICECONTRACT_NAMESPACE, PropertyScope.SESSION));
		this.put(VPUtil.SENDER_IP_ADRESS, (String) message.getProperty(VPUtil.SENDER_IP_ADRESS, PropertyScope.SESSION));
		
		// extract inbound/invocation scoped data
		{
			String httpXForwardedProto = message.getInvocationProperty(VPUtil.VP_X_FORWARDED_PROTO);
			if (httpXForwardedProto != null) {
				this.put(VPUtil.VP_X_FORWARDED_PROTO, httpXForwardedProto);
				// only log on first occasion
				message.removeProperty(VPUtil.VP_X_FORWARDED_PROTO, PropertyScope.INVOCATION);
			}
			String httpXForwardedHost = message.getInvocationProperty(VPUtil.VP_X_FORWARDED_HOST);
			if (httpXForwardedHost != null) {
				this.put(VPUtil.VP_X_FORWARDED_HOST, httpXForwardedHost);
				// only log on first occasion
				message.removeProperty(VPUtil.VP_X_FORWARDED_HOST, PropertyScope.INVOCATION);
			}
			String httpXForwardedPort = message.getInvocationProperty(VPUtil.VP_X_FORWARDED_PORT);
			if (httpXForwardedPort != null) {
				this.put(VPUtil.VP_X_FORWARDED_PORT, httpXForwardedPort);
				// only log on first occasion
				message.removeProperty(VPUtil.VP_X_FORWARDED_PORT, PropertyScope.INVOCATION);
			}
		}
		
		// extract MDC data
		if (MdcLogTrace.get(MdcLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE) != null) {
			this.put(MdcLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE, MdcLogTrace.get(MdcLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE));
			this.put(MdcLogTrace.ROUTER_RESOLVE_ANROPSBEHORIGHET_TRACE, MdcLogTrace.get(MdcLogTrace.ROUTER_RESOLVE_ANROPSBEHORIGHET_TRACE));
		}
		
		String endpoint = message.getProperty(VPUtil.ENDPOINT_URL, PropertyScope.SESSION);
		if (endpoint != null) {
			this.put(VPUtil.ENDPOINT_URL, endpoint);
		}
		final Boolean error = (Boolean) message.getProperty(VPUtil.SESSION_ERROR, PropertyScope.SESSION);
		if (Boolean.TRUE.equals(error)) {
			this.put(VPUtil.SESSION_ERROR, error.toString());
			this.put(VPUtil.SESSION_ERROR_DESCRIPTION,
					VPUtil.nvl((String) message.getProperty(VPUtil.SESSION_ERROR_DESCRIPTION, PropertyScope.SESSION)));
			this.put(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION,
					VPUtil.nvl((String) message.getProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, PropertyScope.SESSION)));
			this.put(VPUtil.SESSION_ERROR_CODE,
					VPUtil.nvl((String) message.getProperty(VPUtil.SESSION_ERROR_CODE, PropertyScope.SESSION)));
			this.putIfNotEmpty(VPUtil.SESSION_HTML_STATUS,
					VPUtil.nvl(message.getProperty(VPUtil.SESSION_HTML_STATUS, PropertyScope.SESSION)));
		}
	}
	
	public void addSource(String className) {
		this.put("source", className);
	}
	
	private void putIfNotEmpty(String key, String value) {
		if(value != null && value.length() > 0)
			this.put(key, value);
	}
				
}
