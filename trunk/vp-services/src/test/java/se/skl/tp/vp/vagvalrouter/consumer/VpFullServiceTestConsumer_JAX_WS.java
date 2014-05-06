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
package se.skl.tp.vp.vagvalrouter.consumer;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.wsaddressing10.AttributedURIType;

import se.skl.tjanst1.wsdl.GetProductDetailResponse;
import se.skl.tjanst1.wsdl.GetProductDetailType;
import se.skl.tjanst1.wsdl.Product;
import se.skl.tjanst1.wsdl.Tjanst1Interface;
import se.skl.tjanst1.wsdl.Tjanst1Service;
import se.skl.tp.vp.util.ClientUtil;

/**
 * Test consumer based on JAX WS.
 * Works as a standalone test client but not to be used in a mule integration test, since it will fail on two mule https connectors.
 * 
 * @author magnuslarsson
 *
 */
public class VpFullServiceTestConsumer_JAX_WS {
	
	private static final Logger logger = LoggerFactory.getLogger(VpFullServiceTestConsumer_JAX_WS.class);

	public static void main(String[] args) {

		if (args.length == 0) {
			args = new String[] { "SW123", "https://localhost:20000/vp/tjanst1" };

		} else if (args.length != 2) {
			throw new RuntimeException("Invalide number of arguments, parameters: productId");
		}

		// Needed for accessing the WSDL file from an https URL
		System.setProperty("javax.net.ssl.keyStore", "../certs/tp.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("javax.net.ssl.trustStore", "../certs/truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

		// Loads a cxf configuration file to use
		SpringBusFactory bf = new SpringBusFactory();
		URL busFile = ClassLoader.getSystemResource("cxf-https.xml");
		Bus bus = bf.createBus(busFile.toString());
		SpringBusFactory.setDefaultBus(bus);

		String productId = args[0];
		String serviceAddress = args[1];

		Product p = null;
		;
		try {
			p = callGetProductDetail(productId, serviceAddress);
			System.out.println("Product Data: " + p.getId() + " - " + p.getDescription() + " - "
					+ p.getHeight() + " - " + p.getWidth());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Product callGetProductDetail(String productId, String serviceAddress)
			throws Exception {

		URL resource = VpFullServiceTestConsumer_JAX_WS.class.getClassLoader().getResource(".");//Thread.currentThread().getContextClassLoader().getResource(".");
		System.out.println(resource.toString());
		
		
		// Needed for accessing the WSDL file from an https URL
		System.setProperty("javax.net.ssl.keyStore", "../certs/tp.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("javax.net.ssl.trustStore", "../certs/truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");
		
		Tjanst1Service ts = new Tjanst1Service(ClientUtil
				.createEndpointUrlFromServiceAddress(serviceAddress));
		Tjanst1Interface serviceInterface = ts.getTjanst1ImplPort();

		GetProductDetailType t = new GetProductDetailType();
		t.setProductId(productId);

		AttributedURIType logicalAddressHeader = new AttributedURIType();
		logicalAddressHeader.setValue("vp-test-producer");


		GetProductDetailResponse response = serviceInterface.getProductDetail(logicalAddressHeader, t);
		Product p = response.getProduct();

		logger.info("Product Data: " + p.getId() + " - " + p.getDescription() + " - "
				+ p.getHeight() + " - " + p.getWidth());
		return p;
	}
}

