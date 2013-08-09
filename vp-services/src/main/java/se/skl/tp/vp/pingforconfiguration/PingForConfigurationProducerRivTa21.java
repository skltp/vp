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
package se.skl.tp.vp.pingforconfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebService;

import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21",
		wsdlLocation = "TD_MONITORING_1_0_0/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP21.wsdl")
public class PingForConfigurationProducerRivTa21 implements PingForConfigurationResponderInterface {

	@Override
	public PingForConfigurationResponseType pingForConfiguration(
			String logicalAddress, PingForConfigurationType parameters) {
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		
		response.setPingDateTime(formatter.format(new Date()));
		response.setVersion("V1.0");
		
		return response;
	}

}
