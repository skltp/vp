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
package se.skl.tp.vp.monitoring.ping;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.transport.http.ReleasingInputStream;
import org.soitoolkit.commons.mule.rest.RestClient;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;

import se.skl.tp.vp.VpMuleServer;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;

public class MonitorPingIntegrationTest extends AbstractTestCase{
	
	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
	
	public MonitorPingIntegrationTest() {
		super();

		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
	}
	
	@BeforeClass
	public static void setupTjanstekatalogen() throws Exception {
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord("LOGICAL_ADDRESS_1","https://localhost:19000/vardgivare-b/tjanst1"));
		vagvalInputs.add(createVagvalRecord("LOGICAL_ADDRESS_2", "https://www.google.com:81"));
		svimi.setVagvalInputs(vagvalInputs);
	}

	@Override
	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml,"
				+ "vp-common.xml,"
				+ "services/PingService-service.xml,"
				+ "services/VagvalRouter-service.xml,"
				+ "services/PingForConfiguration-rivtabp21-service.xml,"
				+ "vp-teststubs-and-services-config.xml";
	}
	
	

	@Test
	public void monitorPing() throws Exception {
		MuleMessage message = new RestClient(muleContext).doHttpGetRequest_JsonContent(VpMuleServer.getAddress("PINGSERVICE_INBOUND_URL"));
		String payload = payloadToString(message);
		assertEquals("TP is alive!", payload);
	}

	private String payloadToString(MuleMessage message) {
		ReleasingInputStream payload = (ReleasingInputStream) message.getPayload();
		Reader reader = new InputStreamReader(payload);
		int data = 0;
		try {
			data = reader.read();
		} catch (IOException e) {}
		
		String payloadStr = "";
		while (data != -1) {
			char theChar = (char) data;
			payloadStr += theChar;
			try {
				data = reader.read();
			} catch (IOException e) {}
		}
		try {
			reader.close();
		} catch (IOException e) {}
		return payloadStr;
	}
	
	private static VagvalMockInputRecord createVagvalRecord(String receiverId, String adress) {
		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.senderId = "tp";
		vagvalInput.rivVersion = "RIVTABP20";
		vagvalInput.serviceContractNamespace = "urn:riv:domain:subdomain:GetProductDetailResponder:1";
		vagvalInput.adress = adress;
		return vagvalInput;
	}
}
