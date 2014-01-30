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

import java.util.ArrayList;
import java.util.List;

import org.mule.tck.FunctionalTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class VpFullServiceTest extends FunctionalTestCase {

	private static final int    CLIENT_TIMEOUT_MS = 60000;
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	private static final String TJANSTE_ADRESS_SHORT_TIMEOUT  = "https://localhost:20000/vp/tjanst1-short-timeout";
	private static final String TJANSTE_ADRESS_LONG_TIMEOUT   = "https://localhost:20000/vp/tjanst1-long-timeout";
	private static final String LOGICAL_ADDRESS               = "vp-test-producer";
	private static final String LOGICAL_ADDRESS_NO_CONNECTION = "vp-test-producer-no-connection";

    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config-override");

	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	private int normal_timeout_ms = 0;
	private int short_timeout_ms = 0;
	private int long_timeout_ms = 0;
	
	public VpFullServiceTest() {
		super();
		
		setDisposeManagerPerSuite(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();

		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS,               "https://localhost:19000/vardgivare-b/tjanst1"));
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_NO_CONNECTION, "https://www.google.com:81"));

		svimi.setVagvalInputs(vagvalInputs);
		
		normal_timeout_ms = Integer.parseInt(rb.getString("TEST_NORMAL_TIMEOUT_MS"));
		short_timeout_ms = Integer.parseInt(rb.getString("TEST_SHORT_TIMEOUT_MS"));
		long_timeout_ms = Integer.parseInt(rb.getString("TEST_LONG_TIMEOUT_MS"));
	}

	private VagvalMockInputRecord createVagvalRecord(String receiverId, String adress) {
		VagvalMockInputRecord vi_TP = new VagvalMockInputRecord();
		vi_TP.receiverId = receiverId;
		vi_TP.senderId = "tp";
		vi_TP.rivVersion = "RIVTABP20";
		vi_TP.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi_TP.adress = adress;
		return vi_TP;
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@Override
	protected void doSetUp() throws Exception {
		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
		}
	}

	public void testHappyDays() throws Exception {
		
		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
		assertEquals(PRODUCT_ID, p.getId());
	}

	public void testShortConnectionTimeout() throws Exception {
		
		long ts = System.currentTimeMillis();
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_SHORT_TIMEOUT, LOGICAL_ADDRESS_NO_CONNECTION);
			fail("An timeout should have occurred");
		} catch (Throwable e) {
			ts = System.currentTimeMillis() - ts;
			assertTrue("Expected time to be between short_timeout_ms (" + short_timeout_ms + ") and normal_timeout_ms (" + normal_timeout_ms + ") but was " + ts + " ms.", short_timeout_ms < ts && ts < normal_timeout_ms);
		}
	}

	public void testLongConnectionTimeout() throws Exception {
		
		long ts = System.currentTimeMillis();
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_LONG_TIMEOUT, LOGICAL_ADDRESS_NO_CONNECTION);
			fail("An timeout should have occurred");
		} catch (Throwable e) {
			ts = System.currentTimeMillis() - ts;
			assertTrue("Expected time to be longer than long_timeout_ms (" + long_timeout_ms + ") but was " + ts + " ms.", ts > long_timeout_ms);
		}
	}
}