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
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.riv.itintegration.monitoring.v1.ConfigurationType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21",
		wsdlLocation = "TD_MONITORING_1_0_0/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP21.wsdl")
public class PingForConfigurationProducerRivTa21 implements PingForConfigurationResponderInterface {
	
	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationProducerRivTa21.class);
	
	private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config","vp-config-override");

	@Lookup("vagvalAgent")
	private VagvalAgent vagvalAgent;

	@Override
	public PingForConfigurationResponseType pingForConfiguration(
			String logicalAddress, PingForConfigurationType parameters) {
		
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		Integer authInfoSize = vagvalAgent.getAnropsBehorighetsInfoList().size();
		Integer routingInfoSize = vagvalAgent.getVirtualiseringsInfo().size();
		
		if(log.isDebugEnabled()){
			log.debug("pingForConfiguration result, nr of authorizations:{}, nr of routing information:{}", authInfoSize.toString(), routingInfoSize.toString());
		}
		
		if(!resourcesNeededForVpAvailable()){
			log.error("Severe problem, vp reports needed resources are missing, routing size: {}", vagvalAgent.getVirtualiseringsInfo().size());
			log.error("Severe problem, vp reports needed resources are missing, authorization size: {}", vagvalAgent.getAnropsBehorighetsInfoList().size());
			throw new RuntimeException("VP012 Severe problem, vp does not have all necessary reources to operate");
		}
		
		response.getConfiguration().add(createConfiguration("Applikation", "VP"));
		response.getConfiguration().add(createConfiguration("Anropsbehörigheter", authInfoSize.toString()));
		response.getConfiguration().add(createConfiguration("Logiska-adresser", routingInfoSize.toString()));
		response.setPingDateTime(formatter.format(new Date()));
		response.setVersion(rb.getString("VP_VERSION"));
		
		return response;
	}

	boolean resourcesNeededForVpAvailable() {
		return vagvalAgent != null
				&& vagvalAgent.getAnropsBehorighetsInfoList().size() > 0
				&& vagvalAgent.getVirtualiseringsInfo().size() > 0;
	}

	private ConfigurationType createConfiguration(String name, String value) {
		ConfigurationType configurationType = new ConfigurationType();
		configurationType.setName(name);
		configurationType.setValue(value);
		return configurationType;
	}
	
	void setVagvalAgent(VagvalAgent vagvalAgent){
		this.vagvalAgent = vagvalAgent;
	}

}
