/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheImpl;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;

public class VagvalAgentTest {

	VagvalAgentMock vagvalAgent;

	private static final String CRM_SCHEDULING = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
	private static final String CRM_LISTING = "urn:riv:crm:listing:GetListingResponder:1";
	private static final String RIVTABP21 = "rivtabp21";
	private static final String NATIONAL_CONSUMER = "NationellKonsument";
	private static final String LOCAL_CONSUMER = "LokalKonsument";

	private static final String HEALTHCAREUNIT_A = "SE0000000001-1234";
	private static final String HEALTHCAREPROVIDER_A = "SE0000000002-1234";
	private static final String HEALTHCAREPROVIDER_B = "SE0000000005-1234";
	private static final String UNKNOWN_HEALTHCAREPROVIDER = "UNKNOWN";
	private static final String SE = "SE";

	@Before
	public void beforeTest() throws Exception {

		URL url = getClass().getClassLoader().getResource("hsacache.xml");
		HsaCache hsaCache = new HsaCacheImpl().init(url.getFile());

		vagvalAgent = new VagvalAgentMock();
		vagvalAgent.setAddressDelimiter("#");
		vagvalAgent.setHsaCache(hsaCache);
	}

	@Test
	public void nationalRoutingAndAuthorizationNationalConsumer() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.getVirtualiseringsInfo().size());
		assertEquals("https://SE", response.getVirtualiseringsInfo().get(0).getAdress());
		assertEquals(SE, response.getVirtualiseringsInfo().get(0).getReceiverId());
		assertEquals(RIVTABP21, response.getVirtualiseringsInfo().get(0).getRivProfil());
		assertEquals(CRM_SCHEDULING, response.getVirtualiseringsInfo().get(0).getTjansteKontrakt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getFromTidpunkt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getTomTidpunkt());
	}

	@Test
	public void unknownRecieverGivesRoutingToSEWhenAuthorizationExist() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(UNKNOWN_HEALTHCAREPROVIDER);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.getVirtualiseringsInfo().size());
		assertEquals("https://SE", response.getVirtualiseringsInfo().get(0).getAdress());
		assertEquals(SE, response.getVirtualiseringsInfo().get(0).getReceiverId());
		assertEquals(RIVTABP21, response.getVirtualiseringsInfo().get(0).getRivProfil());
		assertEquals(CRM_SCHEDULING, response.getVirtualiseringsInfo().get(0).getTjansteKontrakt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getFromTidpunkt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getTomTidpunkt());
	}

	@Test
	public void unknownRecieverGivesNoRoutingToSEWhenAuthorizationDontExist() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(UNKNOWN_HEALTHCAREPROVIDER);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		try {
			vagvalAgent.visaVagval(request);
		} catch (VpSemanticException e) {
			assertEquals(
					"VP007 Authorization missing for serviceNamespace: urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1, receiverId: UNKNOWN, senderId: NationellKonsument",
					e.getMessage());
			return;
		}

		fail("Expected VpSemanticException");
	}

	@Test
	public void nationalRoutingLocalAuthorizationNationalConsumer() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		routing.add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.getVirtualiseringsInfo().size());
		assertEquals("https://providerA", response.getVirtualiseringsInfo().get(0).getAdress());
		assertEquals(HEALTHCAREPROVIDER_A, response.getVirtualiseringsInfo().get(0).getReceiverId());
		assertEquals(RIVTABP21, response.getVirtualiseringsInfo().get(0).getRivProfil());
		assertEquals(CRM_SCHEDULING, response.getVirtualiseringsInfo().get(0).getTjansteKontrakt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getFromTidpunkt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getTomTidpunkt());
	}

	@Test
	public void nationalRoutingLocalAuthorizationLocalConsumer() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_A);
		request.setSenderId(LOCAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.getVirtualiseringsInfo().size());
		assertEquals("https://SE", response.getVirtualiseringsInfo().get(0).getAdress());
		assertEquals(SE, response.getVirtualiseringsInfo().get(0).getReceiverId());
		assertEquals(RIVTABP21, response.getVirtualiseringsInfo().get(0).getRivProfil());
		assertEquals(CRM_SCHEDULING, response.getVirtualiseringsInfo().get(0).getTjansteKontrakt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getFromTidpunkt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getTomTidpunkt());
	}

	@Test
	public void localConsumerWithNoAccessToLocalHealthCareProvider() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		routing.add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));
		routing.add(createRouting("https://providerB", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_A);
		request.setSenderId(LOCAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		try {
			vagvalAgent.visaVagval(request);
		} catch (VpSemanticException e) {
			assertEquals(
					"VP007 Authorization missing for serviceNamespace: urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1, receiverId: SE0000000002-1234, senderId: LokalKonsument",
					e.getMessage());
			return;
		}

		fail("Expected VpSemanticException");
	}

	@Test
	public void localConsumerWithAccessOnlyToHealthcareProvider() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		routing.add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));
		routing.add(createRouting("https://providerB", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_A);
		request.setSenderId(LOCAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.getVirtualiseringsInfo().size());
		assertEquals("https://providerA", response.getVirtualiseringsInfo().get(0).getAdress());
		assertEquals(HEALTHCAREPROVIDER_A, response.getVirtualiseringsInfo().get(0).getReceiverId());
		assertEquals(RIVTABP21, response.getVirtualiseringsInfo().get(0).getRivProfil());
		assertEquals(CRM_SCHEDULING, response.getVirtualiseringsInfo().get(0).getTjansteKontrakt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getFromTidpunkt());
		assertNotNull(response.getVirtualiseringsInfo().get(0).getTomTidpunkt());
	}

	@Test
	public void deprecatedDefaultRoutingDontUseHsaTreeForRouting() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		routing.add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		/*
		 * HEALTHCAREPROVIDER_B#HEALTHCAREUNIT_A, means that first a check is
		 * done to see if any routing exist for HEALTHCAREUNIT_A then on
		 * HEALTHCAREPROVIDER_B. If HSA tree was used routing would have been
		 * found for HEALTHCAREPROVIDER_A which is a parent to HEALTHCAREUNIT_A.
		 */
		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_B + "#" + HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);
		assertTrue(response.getVirtualiseringsInfo().isEmpty());

	}

	@Test
	public void deprecatedDefaultRoutingDontUseHsaTreeForAuthorization() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		routing.add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREUNIT_A));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		/*
		 * HEALTHCAREPROVIDER_B#HEALTHCAREUNIT_A, means that first a check is
		 * done to see if any authorization exist for HEALTHCAREUNIT_A then on
		 * HEALTHCAREPROVIDER_B. If HSA tree was used authorization would have
		 * been found for HEALTHCAREPROVIDER_A which is a parent to
		 * HEALTHCAREUNIT_A.
		 */
		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_B + "#" + HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		try {
			vagvalAgent.visaVagval(request);
		} catch (VpSemanticException e) {
			assertEquals(
					"VP007 Authorization missing for serviceNamespace: urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1, receiverId: SE0000000005-1234#SE0000000001-1234, senderId: NationellKonsument",
					e.getMessage());
			return;
		}

		fail("Expected VpSemanticException");
	}

	@Test
	public void noRoutingExistAtAllEvenWhenUsingHsaTree() throws Exception {

		// No routing for CRM_SCHEDULING, only CRM_LISTING
		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));

		vagvalAgent.setMockVirtualiseringsInfo(routing);
		vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		VisaVagvalResponse response = vagvalAgent.visaVagval(request);

		assertEquals("No routing found", 0, response.getVirtualiseringsInfo().size());
	}

	private XMLGregorianCalendar createTimestamp() throws Exception {
		XMLGregorianCalendar time = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);
		time.add(anHourAgo);
		return time;
	}
}
