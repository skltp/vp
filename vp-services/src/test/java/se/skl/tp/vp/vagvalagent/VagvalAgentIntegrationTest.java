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
package se.skl.tp.vp.vagvalagent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.AN_HOUR_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_ONE_HOUR;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_TEN_YEARS;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.TWO_HOURS_AGO;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VagvalSchemasTestUtil;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCacheLog;

public class VagvalAgentIntegrationTest extends AbstractTestCase {

	private VagvalAgent vagvalAgent;

	private static final String vardgivareC = "SE0000000055-1234";
	private static final String vardgivareB = "SE0000000003-1234";
	private static final String vardenhetA = "SE0000000001-1234";

	private static final String konsumentA = "konsumentA";

	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();

	public VagvalAgentIntegrationTest() {
		super();
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
//		setDisposeContextPerClass(false);
	}


	@Override
	protected String[] getConfigFiles() {
		return new String[]{"soitoolkit-mule-jms-connector-activemq-embedded.xml",
			"vp-common.xml",
			"services/VagvalRouter-service.xml",
			"vp-teststubs-and-services-config.xml"};
	}

	private void setupTjanstekatalogen() throws Exception {
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<>();
		vagvalInputs.add(createVagvalRecord(vardgivareB, "rivtabp20", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		vagvalInputs.add(createVagvalRecord(vardgivareB, "rivtabp21", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		vagvalInputs.add(createVagvalRecordValidBefore(vardgivareC, "rivtabp21", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		vagvalInputs.add(createVagvalRecordValidLater(vardgivareC, "rivtabp21", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		svimi.setVagvalInputs(vagvalInputs);
	}

	@Override
	protected void doSetUpBeforeMuleContextCreation() throws Exception {
		setupTjanstekatalogen();
	}
	
	@Override
	public void doSetUp() throws Exception {
		vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject("vagvalAgent");
	}

	@Test
	public void testGiltigaVagval() throws Exception {

		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, routingInfos.size());
	}

	@Test
	public void testGiltigaVagvalDelimiter() throws Exception {

		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB + "#"
				+ vardenhetA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, routingInfos.size());
	}

	@Test
	public void testVpSemanticExceptionVP007WhenUnauthorized() throws Exception {

		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = "XXX";

		try {
			vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, vardgivareB, tjansteKontrakt));
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertEquals("VP007 Authorization missing for serviceNamespace: urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1, receiverId: SE0000000003-1234, senderId: XXX", e.getMessage());
		}
	}

	@Test
	public void testGiltigaVagvalIngetKontrakt() throws Exception {
		String tjansteKontrakt = "XXX";
		String senderId = konsumentA;
		String receiverId = vardgivareB;

		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId,tjansteKontrakt));
		assertEquals("Wrong number of routings found for service contract", 0, routingInfos.size());
	}

	@Test
	public void testGiltigaVagvalIngenMottagare() throws Exception {
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = konsumentA;
		String receiverId = "XXX";

		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId,tjansteKontrakt));
		assertEquals("Wrong number of routings found for receiver", 0, routingInfos.size());
	}

	@Test
	public void testVagvalNotValidAtThisTime() throws Exception {
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = konsumentA;
		String receiverId = vardgivareC;
		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId, tjansteKontrakt));
		assertEquals("Wrong amount of routings found for the duration", 0, routingInfos.size());
	}

	@Test
	public void resetCacheUpdatesVagvalAgent() throws Exception {
		List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals("Wrong number of routings found in TAK", 2, routingInfos.size());

		String receiverId = vardgivareB;
		String rivVersion = "rivtabp20";
		String senderId = konsumentA;
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";

		svimi.getVagvalInputs().add(createVagvalRecord(receiverId, rivVersion, senderId, tjansteKontrakt));

		TakCacheLog rvcResponse = vagvalAgent.resetVagvalCache();
		assertTrue("Reset cache failed", rvcResponse.isRefreshSuccessful());

		routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals("Wrong number of routings after reset cache", 3, routingInfos.size());
	}

	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId,
			String tjansteGranssnitt) {

		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);
		return vvR;
	}

	private static VagvalMockInputRecord createVagvalRecord(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {

		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.rivVersion = rivVersion;
		vagvalInput.senderId = senderId;
		vagvalInput.serviceContractNamespace = serviceNameSpace;
		return vagvalInput;
	}

	private static VagvalMockInputRecord createVagvalRecordValidBefore(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {
		VagvalMockInputRecord vagvalInput = createVagvalRecord(receiverId,rivVersion,senderId,serviceNameSpace);
		vagvalInput.setFromDate(VagvalSchemasTestUtil.getRelativeDate(TWO_HOURS_AGO));
		vagvalInput.setToDate(VagvalSchemasTestUtil.getRelativeDate(AN_HOUR_AGO));
		return vagvalInput;
	}

	private static VagvalMockInputRecord createVagvalRecordValidLater(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {
		VagvalMockInputRecord vagvalInput = createVagvalRecord(receiverId,rivVersion,senderId,serviceNameSpace);
		vagvalInput.setFromDate(VagvalSchemasTestUtil.getRelativeDate(IN_ONE_HOUR));
		vagvalInput.setToDate(VagvalSchemasTestUtil.getRelativeDate(IN_TEN_YEARS));
		return vagvalInput;
	}
}
