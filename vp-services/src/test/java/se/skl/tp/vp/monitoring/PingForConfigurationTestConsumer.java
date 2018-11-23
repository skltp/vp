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

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.riv.itintegration.monitoring.rivtabp21.v1.PingForConfigurationResponderInterface;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;

public class PingForConfigurationTestConsumer {

	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationTestConsumer.class);

	PingForConfigurationResponderInterface _service;

	public PingForConfigurationTestConsumer(String serviceAddress) {
		JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
		proxyFactory.setServiceClass(PingForConfigurationResponderInterface.class);
		proxyFactory.setAddress(serviceAddress);

		_service = (PingForConfigurationResponderInterface) proxyFactory.create();
	}

	public PingForConfigurationResponseType callService(String logicalAddress) throws Exception {
		log.debug("Calling PingForConfiguration soap-service with logicalAddress = {}", logicalAddress);
		PingForConfigurationType request = new PingForConfigurationType();
		request.setLogicalAddress(logicalAddress);
		request.setServiceContractNamespace("urn:riv:itintegration:monitoring:PingForConfigurationResponder:1");
		return _service.pingForConfiguration(logicalAddress, request);
	}

}
