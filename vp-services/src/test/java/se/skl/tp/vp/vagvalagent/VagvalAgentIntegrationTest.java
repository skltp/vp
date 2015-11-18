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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skltp.tak.vagval.wsdl.v2.ResetVagvalCacheRequest;
import se.skltp.tak.vagval.wsdl.v2.ResetVagvalCacheResponse;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;

public class VagvalAgentIntegrationTest extends AbstractTestCase {

	private VagvalAgent vagvalAgent;

	private static final String vardgivareB = "SE0000000003-1234";
	private static final String vardenhetA = "SE0000000001-1234";

	private static final String konsumentA = "konsumentA";

	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();

	public VagvalAgentIntegrationTest() {
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

	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();
		
		vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject("vagvalAgent");
		
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(vardgivareB, "rivtabp20", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		vagvalInputs.add(createVagvalRecord(vardgivareB, "rivtabp21", konsumentA, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		svimi.setVagvalInputs(vagvalInputs);
		
		vagvalAgent.resetVagvalCache(createResetVagvalCacheRequest());
	}
	
	@After
	public void doTearDown() throws Exception {
		svimi.reset();
	}

	@Test
	public void testGiltigaVagval() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalDelimiter() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB + "#"
				+ vardenhetA, null, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testVpSemanticExceptionVP007WhenUnauthorized() throws Exception {

		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = "XXX";
		String receiverId = vardgivareB;
		Duration duration = null;

		try {
			vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId, duration, tjansteKontrakt));
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
		Duration duration = null;

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId, duration,tjansteKontrakt));
		assertEquals("Wrong number of routings found for service contract", 0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalIngenMottagare() throws Exception {
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = konsumentA;
		String receiverId = "XXX";
		Duration duration = null;

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId, duration,tjansteKontrakt));
		assertEquals("Wrong number of routings found for receiver", 0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalOgiltigTid() throws Exception {
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
		String senderId = konsumentA;
		String receiverId = vardgivareB;
		Duration twentyYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true, new BigInteger("20"),
				new BigInteger("2"));

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(senderId, receiverId, twentyYearsDuration,tjansteKontrakt));
		assertEquals("Wrong amount of routings found for the duration", 0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void resetCacheUpdatesVagvalAgent() throws Exception {
		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals("Wrong number of routings found in TAK", 2, vvResponse.getVirtualiseringsInfo().size());

		String receiverId = vardgivareB;
		String rivVersion = "rivtabp20";
		String senderId = konsumentA;
		String tjansteKontrakt = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";

		svimi.getVagvalInputs().add(createVagvalRecord(receiverId, rivVersion, senderId, tjansteKontrakt));

		ResetVagvalCacheResponse rvcResponse = vagvalAgent.resetVagvalCache(createResetVagvalCacheRequest());
		assertTrue("Reset cache failed", rvcResponse.isResetResult());

		vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals("Wrong number of routings after reset cache", 3, vvResponse.getVirtualiseringsInfo().size());
	}

	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId, Duration addToTidPunkt,
			String tjansteGranssnitt) {

		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);
		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		if (addToTidPunkt != null) {
			tidPunkt.add(addToTidPunkt);
		}
		vvR.setTidpunkt(tidPunkt);
		return vvR;
	}

	private ResetVagvalCacheRequest createResetVagvalCacheRequest() {
		ResetVagvalCacheRequest rvcR = new ResetVagvalCacheRequest();
		return rvcR;
	}

	private static VagvalMockInputRecord createVagvalRecord(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {

		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.rivVersion = rivVersion;
		vagvalInput.senderId = senderId;
		vagvalInput.serviceContractNamespace = serviceNameSpace;
		return vagvalInput;
	}

}
