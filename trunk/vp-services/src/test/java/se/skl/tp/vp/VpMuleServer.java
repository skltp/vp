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

 
import java.util.ArrayList;
import java.util.List;

import org.soitoolkit.commons.mule.test.StandaloneMuleServer;

import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;


public class VpMuleServer {


	public static final String MULE_SERVER_ID   = "vp";
 
	public static final String MULE_CONFIG      = "vp-teststubs-and-services-config.xml"; // both teststubs and services
//	public static final String MULE_CONFIG      = "vp-teststubs-only-config.xml"; // only teststubs
//	public static final String MULE_CONFIG      = "vp-config.xml"; // only services
	
	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();

	public static void main(String[] args) throws Exception {
 
		initTk();
		
		StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, MULE_CONFIG, true);
 
		muleServer.run();
	}
	
	static private void initTk() {
		// NOTE this test user the same certificates for consumer,
		// virtualisation-plattform and producer
		// The certs are located in certs folder and has SERIALNUMBER=tp

		// Initialize the vagvalsinfo that is supposed to be in Tjanstekatalogen
		// when the call
		// to the virtual service is made
		// Note certificate serial number is used as sender
		
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		
		//Test producers for urn:skl:tjanst1:rivtabp20
		vagvalInputs.add(createVagvalRecord("vp-test-producer", "RIVTABP20", "tp", "urn:skl:tjanst1:rivtabp20","https://localhost:19000/vardgivare-b/tjanst1"));
		vagvalInputs.add(createVagvalRecord("vp-test-producer-no-connection", "RIVTABP20", "tp", "urn:skl:tjanst1:rivtabp20","https://www.google.com:81"));
		
		//Ping virtual service
		vagvalInputs.add(createVagvalRecord("ping", "RIVTABP20", "tp", "urn:riv:itinfra:tp:Ping:1:rivtabp20","http://localhost:10000/test/Ping_Service"));
		
		//Monitor ping rest service, used by e.g load balancers and monitoring software
		vagvalInputs.add(createVagvalRecord("TEST_SERVICE_HSA_ID", "RIVTABP20", "tp", "urn:riv:itinfra:tp:Ping:1:rivtabp20","http://localhost:10000/test/Ping_Service"));
		
		svimi.setVagvalInputs(vagvalInputs);
	}
	
	private static VagvalMockInputRecord createVagvalRecord(String receiverId, String rivVersion, String senderId, String serviceNameSpace, String adress) {
		
		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.rivVersion = rivVersion;
		vagvalInput.senderId = senderId;
		vagvalInput.serviceNamespace = serviceNameSpace;
		vagvalInput.adress = adress;
		return vagvalInput;
	}
}