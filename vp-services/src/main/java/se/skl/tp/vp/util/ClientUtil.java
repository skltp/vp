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
package se.skl.tp.vp.util;

import java.net.MalformedURLException;
import java.net.URL;

public class ClientUtil {
    
    /**
     * 
     * @param adressOfWsdl, e.g. http://localhost:8080/tppoc-vagvalsinfo-module-web-g/services/SokVagvalsInfoService?wsdl
     * @return
     */
	public static URL createEndpointUrlFromWsdl(String adressOfWsdl) {
		try {
			return new URL(adressOfWsdl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}	

	/**
	 * 
	 * @param serviceAddress, e.g. http://localhost:8080/tppoc-vagvalsinfo-module-web-g/services/SokVagvalsInfoService
	 * @return
	 */
	public static URL createEndpointUrlFromServiceAddress(String serviceAddress) {
		return createEndpointUrlFromWsdl(serviceAddress + "?wsdl");
	}	

}
