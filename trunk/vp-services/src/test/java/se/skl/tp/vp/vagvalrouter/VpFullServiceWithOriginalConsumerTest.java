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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;
import se.skl.tp.vp.vagvalrouter.producer.VpTestProducerLogger;

public class VpFullServiceWithOriginalConsumerTest extends AbstractTestCase {

	private static final String AUHTORIZED_CONSUMER_HSAID = "tp";
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	private static final String LOGICAL_ADDRESS = "vp-test-producer";
	
	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;
	
	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
	
	public VpFullServiceWithOriginalConsumerTest() {
		super();
		
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@BeforeClass
	public static void setupTjanstekatalogen() throws Exception {
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(AUHTORIZED_CONSUMER_HSAID, LOGICAL_ADDRESS, "https://localhost:19000/vardgivare-b/tjanst1"));
		svimi.setVagvalInputs(vagvalInputs);
	}
	
	@Before
	public void doSetUp() throws Exception {
		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector");
		}
	}

	@Test
	public void testOriginalConsumerIsSetToSameAsSenderIfNotProvided() throws Exception {
		
		Map<String, String> properties = new HashMap<String, String>();
    	
    	Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
		assertEquals(PRODUCT_ID, p.getId());
		
		assertEquals("tp", VpTestProducerLogger.getLatestRivtaOriginalSenderId());
		assertEquals("tp", VpTestProducerLogger.getLatestSenderId());
	}
	
	@Test
	public void testOriginalConsumerIsForwardedIfProvided() throws Exception {
		
		final String provicedOriginalServiceConsumerHsaId = "HSA-ID-ORGINALCONSUMER";
		
		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(VagvalRouter.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID, provicedOriginalServiceConsumerHsaId);

    	Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
		assertEquals(PRODUCT_ID, p.getId());
		
		assertEquals(provicedOriginalServiceConsumerHsaId, VpTestProducerLogger.getLatestRivtaOriginalSenderId());
		assertEquals("tp", VpTestProducerLogger.getLatestSenderId());
	}
	
	private static VagvalMockInputRecord createVagvalRecord(String senderId, String receiverId, String adress) {
		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.senderId = senderId;
		vagvalInput.rivVersion = "RIVTABP20";
		vagvalInput.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vagvalInput.adress = adress;
		return vagvalInput;
	}
}