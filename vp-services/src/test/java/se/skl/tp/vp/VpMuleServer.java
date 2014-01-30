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
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		VagvalMockInputRecord vi = new VagvalMockInputRecord();
		vi.receiverId = "vp-test-producer";
		vi.senderId = "tp";
		vi.rivVersion = "RIVTABP20";
		vi.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi.adress = "https://localhost:19000/vardgivare-b/tjanst1";
		vagvalInputs.add(vi);

		vi = new VagvalMockInputRecord();
		vi.receiverId = "vp-test-producer-no-connection";
		vi.senderId = "tp";
		vi.rivVersion = "RIVTABP20";
		vi.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi.adress = "https://www.google.com:81";
		vagvalInputs.add(vi);

		
		svimi.setVagvalInputs(vagvalInputs);
	}
}