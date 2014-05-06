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
package se.skl.tp.vp;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

public abstract class AbstractTestConsumer<ServiceInterface> {

	public static final String SAMPLE_ORIGINAL_CONSUMER_HSAID = "sample-original-consumer-hsaid";
	
	protected ServiceInterface _service = null;	

    private Class<ServiceInterface> _serviceType;

    /**
     * Constructs a test consumer with a web service proxy setup for communication using HTTPS with Mutual Authentication
     * 
     * @param serviceType, required to be able to get the generic class at runtime, see http://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
     * @param serviceAddress
     */
	public AbstractTestConsumer(Class<ServiceInterface> serviceType, String serviceAddress) {

		_serviceType = serviceType;
		
		JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
		proxyFactory.setServiceClass(getServiceType());
		proxyFactory.setAddress(serviceAddress);

		// Used for HTTPS
        /*
		SpringBusFactory bf = new SpringBusFactory();
		URL cxfConfig = this.getClass().getClassLoader().getResource("agp-cxf-test-consumer-config.xml");
		if (cxfConfig != null) {
			proxyFactory.setBus(bf.createBus(cxfConfig));
		}
        */

		_service = proxyFactory.create(getServiceType()); 
	}

    Class<ServiceInterface> getServiceType() {
    	return _serviceType;
    }
}
