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

package se.skl.tp.vp.util;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteListHandler {

	private static final Logger log = LoggerFactory.getLogger(WhiteListHandler.class);

	private String [] whiteListArray;
	
	public void setWhiteList(String whiteList) {
		whiteListArray = createWhiteList(whiteList);
	}

	private String [] createWhiteList(String whiteListString) {
		
		// Remove possible inline comment
		whiteListString = VPUtil.trimProperty(whiteListString);
		
		//When no whitelist exist we can not validate incoming ip address
		if (VPUtil.isWhitespace(whiteListString)) {
			log.warn("A check against the ip address whitelist was requested, but the whitelist is configured empty. Update VP configuration property IP_WHITE_LIST");
			return null;
		}
		
		return whiteListString.split(",");

	}
	/**
	 * Check if the calling ip address is on accepted list of ip addresses or subdomains. False
	 * is always returned in case no whitelist exist or ip address is empty.
	 * 
	 * @param callerIp The callers ip
	 * @param whiteList The comma separated list of ip addresses or subdomains 
	 * @param httpHeader The http header causing the check in the white list
	 * @return true if caller is on whitelist
	 */
	public boolean isCallerOnWhiteList(String callerIp, String httpHeader) {
		
		log.debug("Check if caller {} is in white list berfore using HTTP header {}...", callerIp, httpHeader);

		//When no ip address exist we can not validate against whitelist
		if (VPUtil.isWhitespace(callerIp)) {
			log.warn("A potential empty ip address from the caller, ip adress is: {}. HTTP header that caused checking: {} ", callerIp, httpHeader);
			return false;
		}

		
		//When no whitelist exist we can not validate incoming ip address
		if (whiteListArray == null) {
			log.warn("A check against the ip address whitelist was requested, but the whitelist is configured empty. Update VP configuration property IP_WHITE_LIST");
			return false;
		}
		
		
		for (String ipAddress : whiteListArray) {
			if(callerIp.startsWith(ipAddress.trim())){
				log.debug("Caller matches ip address/subdomain in white list");
				return true;
			}
		}

		log.warn("Caller was not on the white list of accepted IP-addresses. IP-address: {}, accepted IP-addresses in IP_WHITE_LIST: {}", callerIp, this.toString());
		return false;
	}

	@Override
	public String toString() {
		return "[" + Arrays.toString(whiteListArray) + "]";
	}
	
	
}
