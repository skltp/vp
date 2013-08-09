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

import org.w3.wsaddressing10.AttributedURIType;

import se.riv.itintegration.monitoring.v1.PingForConfigurationResponderInterface;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.v1.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp20",
		wsdlLocation = "TD_MONITORING_1_0_0/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP20.wsdl")
public class PingForConfigurationProducerRivTa20 implements PingForConfigurationResponderInterface {

	public PingForConfigurationResponseType pingForConfiguration(final AttributedURIType logicalAddress, final PingForConfigurationType parameters) {
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		
		response.setPingDateTime(formatter.format(new Date()));
		response.setVersion("V1.0");
		
		return response;
	}
}
