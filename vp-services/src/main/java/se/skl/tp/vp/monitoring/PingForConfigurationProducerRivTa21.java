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
package se.skl.tp.vp.monitoring;

import java.util.Date;

import javax.inject.Inject;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;
import org.soitoolkit.commons.mule.util.ThreadSafeSimpleDateFormat;

import se.riv.itintegration.monitoring.rivtabp21.v1.PingForConfigurationResponderInterface;
import se.riv.itintegration.monitoring.v1.ConfigurationType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.rivtabp21.v1.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21",
		wsdlLocation = "ServiceContracts_itintegration_monitoring/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP21.wsdl")
public class PingForConfigurationProducerRivTa21 implements PingForConfigurationResponderInterface {
	
	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationProducerRivTa21.class);
	private ThreadSafeSimpleDateFormat dateFormat = new ThreadSafeSimpleDateFormat("yyyyMMddhhmmss");
	private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config","vp-config-override", "vp-build");

	@Inject
	private VagvalAgent vagvalAgent;

	@Override
	public PingForConfigurationResponseType pingForConfiguration(String logicalAddress,
			PingForConfigurationType parameters) {
		
		log.info("PingForConfiguration requested for {}", rb.getString("APPLICATION_NAME"));
		
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		int authInfoSize = vagvalAgent.threadUnsafeLoadBalancerHealthCheckGetNumberOfAnropsBehorigheter();
		int routingInfoSize = vagvalAgent.threadUnsafeLoadBalancerHealthCheckGetNumberOfVirtualizations();
		
		if(log.isDebugEnabled()){
			log.debug("pingForConfiguration result, nr of authorizations:{}, nr of routing information:{}", authInfoSize, routingInfoSize);
		}
		
		if(!resourcesNeededForVpAvailable()){
			log.error("Severe problem, vp reports needed resources are missing, routing size: {}", routingInfoSize);
			log.error("Severe problem, vp reports needed resources are missing, authorization size: {}", authInfoSize);
			throw new RuntimeException(VpSemanticErrorCodeEnum.VP012 + " Severe problem, vp does not have all necessary reources to operate");
		}
		
		response.getConfiguration().add(createConfiguration("Applikation", rb.getString("APPLICATION_NAME")));
		response.getConfiguration().add(createConfiguration("Anropsbehörigheter", String.valueOf(authInfoSize)));
		response.getConfiguration().add(createConfiguration("Logiska-adresser", String.valueOf(routingInfoSize)));
		response.setPingDateTime(dateFormat.format(new Date()));
		response.setVersion(rb.getString("VP_VERSION"));
		
		log.info("PingForConfiguration response returned for {}", rb.getString("APPLICATION_NAME"));
		
		return response;
	}

	boolean resourcesNeededForVpAvailable() {
		return vagvalAgent != null
				&& vagvalAgent.threadUnsafeLoadBalancerHealthCheckGetNumberOfAnropsBehorigheter() > 0
				&& vagvalAgent.threadUnsafeLoadBalancerHealthCheckGetNumberOfVirtualizations() > 0;
	}

	private ConfigurationType createConfiguration(String name, String value) {
		
		log.debug("PingForConfiguration config added [{}: {}]", name, value);
		
		ConfigurationType configurationType = new ConfigurationType();
		configurationType.setName(name);
		configurationType.setValue(value);
		return configurationType;
	}
	
	void setVagvalAgent(VagvalAgent vagvalAgent){
		this.vagvalAgent = vagvalAgent;
	}

}
