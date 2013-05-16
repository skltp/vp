/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */

package se.riv.itinfra.tp.ping;

import java.net.URL;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;

public class PingProducer implements Runnable {

	private static String endpointAddress = "https://localhost:10000/test/Ping_Service";

	protected PingProducer(String address) throws Exception {

		System.out.println("Starting Ping testproducer");

		// Loads a cxf configuration file to use
		final SpringBusFactory bf = new SpringBusFactory();
		final URL busFile = this.getClass().getClassLoader()
				.getResource("cxf-producer.xml");
		final Bus bus = bf.createBus(busFile.toString());

		SpringBusFactory.setDefaultBus(bus);
		final Object implementor = new PingImpl();
		Endpoint.publish(address, implementor);
	}

	@Override
	public void run() {
		System.out.println("Ping testproducer ready...");
	}

	public static void main(String[] args) throws Exception {

		if (args.length > 0) {
			endpointAddress = args[0];
		}

		(new Thread(new PingProducer(endpointAddress))).start();

	}

}
