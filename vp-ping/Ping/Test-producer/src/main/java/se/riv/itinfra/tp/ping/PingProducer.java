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

public class PingProducer {

    protected PingProducer() throws Exception {
        System.out.println("Starting Producer");

        // Loads a cxf configuration file to use
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = this.getClass().getClassLoader().getResource("cxf-producer.xml");
        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        
        Object implementor = new PingImpl();
        String address = "https://localhost:10000/test/Ping_Service";
        Endpoint.publish(address, implementor);
    }

	public static void main(String[] args) throws Exception {
		
        new PingProducer();
        System.out.println("Producer ready...");
        
        Thread.sleep(60 * 60 * 1000);
        System.out.println("Producer exiting");
        System.exit(0);
    }
	
}
