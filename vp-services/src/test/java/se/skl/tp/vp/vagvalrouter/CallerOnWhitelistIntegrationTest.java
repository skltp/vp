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
package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.AbstractJmsTestUtil;
import org.soitoolkit.commons.mule.test.ActiveMqJmsTestUtil;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.skltp.tjanst1.v1.Product;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class CallerOnWhitelistIntegrationTest extends AbstractTestCase {

	private static final int    CLIENT_TIMEOUT_MS = 60000;
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS_HTTP = "http://localhost:8080/vp/tjanst1";
	private static final String LOGICAL_ADDRESS = "vp-test-producer";

    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config", "vp-config-override");

	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	public CallerOnWhitelistIntegrationTest() {
		super();
		
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS, "https://localhost:19000/vardgivare-b/tjanst1"));
		svimi.setVagvalInputs(vagvalInputs);
	}

	private VagvalMockInputRecord createVagvalRecord(String receiverId, String adress) {
		VagvalMockInputRecord vi_TP = new VagvalMockInputRecord();
		vi_TP.receiverId = receiverId;
		vi_TP.senderId = "tp";
		vi_TP.rivVersion = "RIVTABP20";
		vi_TP.serviceContractNamespace = "urn:skl:tjanst1:rivtabp20";
		vi_TP.adress = adress;
		return vi_TP;
	}
	
	@Override
	protected String getConfigResources() {
		
		/*
		 * NOTE! This test uses a separate vp-common configuration file to
		 * be able to mock a different vp-config-override.properties. The
		 * 
		 */
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"test-caller-on-whitelist/test-caller-on-whitelist-vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();

		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPInsecureConnector", CLIENT_TIMEOUT_MS);
		}
	}
	
	/**
	 * Verify that when caller is using http and correct values for http headers 
	 * x-vp-sender-id and x-vp-instance-id, a check is done against ip whitelist.
	 * In this case ip whitelist does not contain 127.0.0.1.
	 */
	@Test
	public void testVP011IsThrownWhenCallerIsNotOnWhitelistUsingHeaderX_VP_SENDER_ID() throws Exception {
		
		final String X_VP_SENDER_ID = "tp";
		final String VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");
		
		/*
		 * Provide a valid vp instance id and x-vp-sender-id to trigger
		 * a check on the ip whitelist.
		 */
 		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(VagvalRouter.X_VP_SENDER_ID, X_VP_SENDER_ID);
    	properties.put(VagvalRouter.X_VP_INSTANCE_ID, VP_INSTANCE_ID);
		
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_HTTP, LOGICAL_ADDRESS, properties);
			fail("Expected error here!");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.1. HTTP header that caused checking: x-vp-sender-id"));
		}
	}
	
	@Test
	public void testVP011IsThrownWhenCallerIsNotOnWhitelistUsingHeader_X_VP_CERT() throws Exception {
			
		/*
		 * Provide a valid cert in http header x-vp-auth-cert to trigger
		 * a check on the ip whitelist.
		 */
 		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(VagvalRouter.REVERSE_PROXY_HEADER_NAME, "kalle");
		
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_HTTP, LOGICAL_ADDRESS, properties);
			fail("Expected error here!");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.1. HTTP header that caused checking: x-vp-auth-cert"));
		}
	}
}