/* 
 * Licensed to the soi-toolkit project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The soi-toolkit project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.skl.tp.vp.util;

import javax.xml.namespace.QName;

import org.mule.api.config.MuleProperties;

/**
 * Utility class for the virtualization platform
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public final class VPUtil {
	
	public static final String REMOTE_ADDR = MuleProperties.MULE_REMOTE_CLIENT_ADDRESS;
	
	public static final String CONSUMER_CONNECTOR_NAME = "VPConsumerConnector";
	
	public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	public static final String REVERSE_PROXY_HEADER_NAME = "x-vp-auth-cert";
	
	public static final String SESSION_ERROR = "sessionStatus";
	public static final String SESSION_ERROR_DESCRIPTION = "sessionErrorDescription";
	public static final String SESSION_ERROR_TECHNICAL_DESCRIPTION = "sessionErrorTechnicalDescription";
	
	public static final String RECEIVER_ID = "receiverid";
	public static final String SENDER_ID = "senderid";
	public static final String RIV_VERSION = "rivversion";
	public static final String SERVICE_NAMESPACE = "cxf_service";
	
	public static final String IS_HTTPS = "isHttps";
	
	public static final String CERT_SENDERID_PATTERN = "=([^,]+)";
	
	public static String extractNamespaceFromService(final QName qname) {
		return qname.getNamespaceURI();
	}
	
	public static String extractIpAddress(final String remoteAddress) {
		final String s = remoteAddress.split(":")[0];
		return s.substring(1, s.length());
	}
	
	public static boolean isWhitespace(final String s) {
		if (s == null) {
			return true;
		}
		
		return s.trim().length() == 0;
	}
}
