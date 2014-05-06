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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;

import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.skl.tp.vp.VpMuleServer;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;

public class PingForConfigurationProducerRivTa21NoTAKIntegrationTest extends AbstractTestCase {
	
	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();

	public PingForConfigurationProducerRivTa21NoTAKIntegrationTest() {
		super();

		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
	}
	
	@BeforeClass
	public static void setupNoInformationInTjanstekatalogen() throws Exception {
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		svimi.setVagvalInputs(vagvalInputs);
	}

	@Override
	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml,"
				+ "vp-common.xml,"
				+ "services/PingForConfiguration-rivtabp21-service.xml,"
				+ "services/VagvalRouter-service.xml,"
				+ "vp-teststubs-and-services-config.xml";
	}
		
	@Test
	public void pingForConfiguration_no_vagval_info_available(){
			
		PingForConfigurationTestConsumer consumer = new PingForConfigurationTestConsumer(VpMuleServer.getAddress("PINGFORCONFIGURATIONSERVICE_RIVTABP21_INBOUND_ENDPOINT"));
		PingForConfigurationType pingRequest = new PingForConfigurationType();
		
		try {
			consumer.callService("LOGICAL_ADDRESS", pingRequest);
			fail("Exception excpected");
		} catch (Exception e) {
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage().contains("VP012 Severe problem, vp does not have all necessary reources to operate"));
		}
	}
}
