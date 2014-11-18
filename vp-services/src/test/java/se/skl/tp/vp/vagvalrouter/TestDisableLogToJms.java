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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

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

public class TestDisableLogToJms extends AbstractTestCase {

	private static final int    CLIENT_TIMEOUT_MS = 60000;
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	private static final String LOGICAL_ADDRESS               = "vp-test-producer";
	private static final String LOGICAL_ADDRESS_NOT_FOUND     = "unknown-logical-address";
	
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle(new String[] {"vp-config-override", "vp-config"});

	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	private static final String LOG_INFO_QUEUE = rb.getString("SOITOOLKIT_LOG_INFO_QUEUE");
	private static final String LOG_ERROR_QUEUE = rb.getString("SOITOOLKIT_LOG_ERROR_QUEUE");
	private AbstractJmsTestUtil jmsUtil = null;
	
	public TestDisableLogToJms() {
		super();
		
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS,               "https://localhost:19000/vardgivare-b/tjanst1"));
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
			"test-disable-log-to-jms/test-disable-log-to-jms-vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();

		// TODO: Fix lazy init of JMS connection et al so that we can create jmsutil in the declaration
		// (The embedded ActiveMQ queue manager is not yet started by Mule when jmsutil is delcared...)
		if (jmsUtil == null) jmsUtil = new ActiveMqJmsTestUtil();
		
		// Clear queues used for the logging
		jmsUtil.clearQueues(LOG_INFO_QUEUE);
		
		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
		}
	}

	/**
	 * Verify that VP send no info log events to JMS when ENABLE_LOG_TO_JMS=false 
	 * @throws Exception
	 */
	@Test
	public void testNoInfoLogToJmsQueue() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());
		
		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
		assertEquals(PRODUCT_ID, p.getId());

		assertEquals("Wrong number of messages on jms queue " + LOG_INFO_QUEUE, 0, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());
	}
	
	/**
	 * Verify that VP send no error log events to JMS when ENABLE_LOG_TO_JMS=false 
	 * @throws Exception
	 */
	@Test
	public void testNoErrorLogToJmsQueue() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
	 	
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
			fail("Expected error here!");
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		assertEquals("Wrong number of messages on jms queue " + LOG_ERROR_QUEUE, 0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
	}
}