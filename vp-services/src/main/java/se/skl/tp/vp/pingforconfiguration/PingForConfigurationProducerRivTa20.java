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
package se.skl.tp.vp.pingforconfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebService;

import org.mule.api.annotations.expressions.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.wsaddressing10.AttributedURIType;

import se.riv.itintegration.monitoring.v1.ConfigurationType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponderInterface;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.v1.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp20",
		wsdlLocation = "TD_MONITORING_1_0_0/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP20.wsdl")
public class PingForConfigurationProducerRivTa20 implements PingForConfigurationResponderInterface {
	
	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationProducerRivTa20.class);

	@Lookup("vagvalAgent")
	private VagvalAgent vagvalAgent;

	public PingForConfigurationResponseType pingForConfiguration(final AttributedURIType logicalAddress, final PingForConfigurationType parameters) {
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		Integer authInfoSize = vagvalAgent.getAnropsBehorighetsInfoList().size();
		Integer routingInfoSize = vagvalAgent.getVirtualiseringsInfo().size();
		
		if(log.isInfoEnabled()){
			log.info("pingForConfiguration result, nr of authorizations:{}, nr of routing information:{}", authInfoSize.toString(), routingInfoSize.toString());
		}
		
		response.getConfiguration().add(createConfiguration("Applikation", "VP"));
		response.getConfiguration().add(createConfiguration("Anropsbehörigheter", authInfoSize.toString()));
		response.getConfiguration().add(createConfiguration("Logiska-adresser", routingInfoSize.toString()));
		response.setPingDateTime(formatter.format(new Date()));
		response.setVersion("V1.0");
		
		return response;
	}
	
	private ConfigurationType createConfiguration(String name, String value) {
		ConfigurationType configurationType = new ConfigurationType();
		configurationType.setName(name);
		configurationType.setValue(value);
		return configurationType;
	}
}
