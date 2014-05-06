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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface;
import se.skl.tp.vp.AbstractTestConsumer;

public class PingForConfigurationTestConsumer  extends AbstractTestConsumer<PingForConfigurationResponderInterface>{

	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationTestConsumer.class);
	
	public PingForConfigurationTestConsumer(String serviceAddress) {	    
		// Setup a web service proxy for communication using HTTPS with Mutual Authentication
		super(PingForConfigurationResponderInterface.class, serviceAddress);
	}
	
	public PingForConfigurationResponseType callService(String logicalAddress, PingForConfigurationType request) {
		log.debug("Calling PingForConfiguration-soap-service ");	
		PingForConfigurationResponseType response = _service.pingForConfiguration(logicalAddress, request);
        return response;
	}

}
