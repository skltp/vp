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

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class VpFullServiceTest extends FunctionalTestCase {

	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	
	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	public VpFullServiceTest() {
		super();
		
		setDisposeManagerPerSuite(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();

		VagvalMockInputRecord vi_TP = new VagvalMockInputRecord();
		vi_TP.receiverId = "vp-test-producer";
		vi_TP.senderId = "tp";
		vi_TP.rivVersion = "RIVTABP20";
		vi_TP.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi_TP.adress = "https://localhost:19000/vardgivare-b/tjanst1";

		vagvalInputs.add(vi_TP);
		svimi.setVagvalInputs(vagvalInputs);

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
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector");
		}
	}

	public void testHappyDays() throws Exception {
		
		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS);
		assertEquals(PRODUCT_ID, p.getId());
	}
}