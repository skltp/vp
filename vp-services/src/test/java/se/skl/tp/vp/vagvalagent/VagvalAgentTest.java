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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.custommonkey.xmlunit.XMLAssert;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheImpl;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.SokVagvalsInfoInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
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

	//JUnit will create a temporary folder before your test, and delete it afterwards
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void beforeTest() throws Exception {

		URL url = getClass().getClassLoader().getResource("hsacache.xml");
		HsaCache hsaCache = new HsaCacheImpl().init(url.getFile());

		File localTakCache = folder.newFile(".tk.localCache");

		vagvalAgent = new VagvalAgentMock();
		vagvalAgent.setAddressDelimiter("#");
		vagvalAgent.setHsaCache(hsaCache);
		vagvalAgent.setLocalTakCache(localTakCache.getAbsolutePath());
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

    @Test
    public void noContactWithTjanstekatalogenAlwaysReultsInLocalCacheIsRead()
            throws Exception {

        SokVagvalsInfoInterface mockedTakService = mockedTakGivesNoResult();

        // The local cache to read when TAK is not available
        URL localTakCache = getClass().getClassLoader().getResource("tklocalcache-test.xml");

        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, localTakCache.getFile());

        //Init VagvalAgent from local cache
        vagvalAgent.init(VagvalAgent.DONT_FORCE_RESET);

        // Verify authorizations are loaded from local cache
        List<AnropsBehorighetsInfoType> authorization = vagvalAgent
                .getAnropsBehorighetsInfoList();

        assertEquals(1, authorization.size());
        assertEquals("vp-test-producer", authorization.get(0).getReceiverId());
        assertEquals("tp", authorization.get(0).getSenderId());
        assertEquals("urn:riv:domain:subdomain:GetProductDetailResponder:1", authorization.get(0)
                .getTjansteKontrakt());

        // Verify virtualizations are loaded from local cache
        List<VirtualiseringsInfoType> virtualizations = vagvalAgent
                .getVirtualiseringsInfo();

        assertEquals(1, virtualizations.size());
        assertEquals("vp-test-producer", virtualizations.get(0).getReceiverId());
        assertEquals("https://localhost:19000/vardgivare-b/tjanst1",
                virtualizations.get(0).getAdress());
        assertEquals("RIVTABP20", virtualizations.get(0).getRivProfil());
        assertEquals("urn:riv:domain:subdomain:GetProductDetailResponder:1", virtualizations.get(0)
                .getTjansteKontrakt());

    }



    @Test
    public void contactWithTjanstekatalogenAlwaysReultsInLocalCacheIsUpdated()
            throws Exception {

        // An empty tmp local cache is created
        File localTakCache = folder.newFile(".tk.empty.localCache");

        // Prepare agent with local cache
        vagvalAgent.setLocalTakCache(localTakCache.getAbsolutePath());

        // Prepare result from TAK that will update the local cache
        ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
        routing.add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

        ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
        authorization.add(createAuthorization(NATIONAL_CONSUMER,
                CRM_SCHEDULING, SE));

        vagvalAgent.setMockVirtualiseringsInfo(routing);
        vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

        vagvalAgent.init(VagvalAgent.DONT_FORCE_RESET);

        XMLAssert.assertXpathExists("/persistentCache/virtualiseringsInfo", new InputSource(new FileReader(localTakCache)));
        XMLAssert.assertXpathExists("/persistentCache/anropsBehorighetsInfo",new InputSource(new FileReader(localTakCache)));
    }

    @Test
    public void contactWithTjanstekatalogenDuringInitIsReportedInProcessingLog()
            throws Exception {

        // An empty tmp local cache is created
        File localTakCache = folder.newFile(".tk.empty.localCache");

        // Prepare agent with local cache
        vagvalAgent.setLocalTakCache(localTakCache.getAbsolutePath());

        // Prepare result from TAK that will update the local cache
        ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
        routing.add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

        ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
        authorization.add(createAuthorization(NATIONAL_CONSUMER,
                CRM_SCHEDULING, SE));

        vagvalAgent.setMockVirtualiseringsInfo(routing);
        vagvalAgent.setMockAnropsBehorighetsInfo(authorization);

        // Init VagvalAgent from local cache
        VagvalAgentProcessingLog processingLog = vagvalAgent
                .init(VagvalAgent.DONT_FORCE_RESET);

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("init: not initialized, will do init ..."));
        assertThat(log.get(1), CoreMatchers.containsString("Host:"));
        assertThat(log.get(2), CoreMatchers.containsString("Time:"));
        assertThat(log.get(3), CoreMatchers.containsString("Initialize VagvalAgent TAK resources.."));
        assertThat(log.get(4), CoreMatchers.containsString("Succeeded to get virtualizations and/or permissions from TAK, save to local TAK copy..."));
        assertThat(log.get(5), CoreMatchers.containsString("Succesfully saved virtualizations and permissions to local TAK copy:"));
        assertThat(log.get(6), CoreMatchers.containsString("Init VagvalAgent loaded number of permissions: 1"));
        assertThat(log.get(7), CoreMatchers.containsString("Init VagvalAgent loaded number of virtualizations: 1"));
        assertThat(log.get(8), CoreMatchers.containsString("init done, was successful: true"));
    }

    @Test
    public void noContactWithTjanstekatalogenDuringInitIsReportedInProcessingLog()
            throws Exception {

        SokVagvalsInfoInterface mockedTakService = mockedTakGivesNoResult();

        // The local cache to read when TAK is not available
        URL localTakCache = getClass().getClassLoader().getResource("tklocalcache-test.xml");

        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, localTakCache.getFile());

        // Init VagvalAgent from local cache
        VagvalAgentProcessingLog processingLog = vagvalAgent
                .init(VagvalAgent.DONT_FORCE_RESET);

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("init: not initialized, will do init ..."));
        assertThat(log.get(1), CoreMatchers.containsString("Host:"));
        assertThat(log.get(2), CoreMatchers.containsString("Time:"));
        assertThat(log.get(3), CoreMatchers.containsString("Initialize VagvalAgent TAK resources.."));
        assertThat(log.get(4), CoreMatchers.containsString("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy..."));
        assertThat(log.get(5), CoreMatchers.containsString("Succesfully restored virtualizations and permissions from local TAK copy:"));
        assertThat(log.get(6), CoreMatchers.containsString("Init VagvalAgent loaded number of permissions: 1"));
        assertThat(log.get(7), CoreMatchers.containsString("Init VagvalAgent loaded number of virtualizations: 1"));
        assertThat(log.get(8), CoreMatchers.containsString("init done, was successful: true"));
    }

    @Test
    public void corruptLocalTakCacheDuringRestoreIsReportedInProcessongLog()
            throws Exception {

        SokVagvalsInfoInterface mockedTakService = mockedTakGivesNoResult();

        // A corrupt local TAK cache
        File corruptLocalTakCache = folder.newFile(".tk.corrupt.localCache");

        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, corruptLocalTakCache.getAbsolutePath());

        // Init VagvalAgent from local cache
        VagvalAgentProcessingLog processingLog = vagvalAgent
                .init(VagvalAgent.DONT_FORCE_RESET);

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("init: not initialized, will do init ..."));
        assertThat(log.get(1), CoreMatchers.containsString("Host:"));
        assertThat(log.get(2), CoreMatchers.containsString("Time:"));
        assertThat(log.get(3), CoreMatchers.containsString("Initialize VagvalAgent TAK resources.."));
        assertThat(log.get(4), CoreMatchers.containsString("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy..."));
        assertThat(log.get(5), CoreMatchers.containsString("Failed to restore virtualizations and permissions from local TAK copy:"));
        assertThat(log.get(6), CoreMatchers.containsString("Reason for failure: javax.xml.bind.UnmarshalException"));
    }

    @Test
    public void noAuthorizationsLoadedGivesEmptyList(){
    	VagvalAgent va = new VagvalAgent();
    	va.setLocalTakCache("non-existing-file.txt");
    	List<AnropsBehorighetsInfoType> authInfo = va.getAnropsBehorighetsInfoList();
    	assertNotNull(authInfo);
    	assertTrue(authInfo.isEmpty());
    }

    @Test
    public void noRoutingLoadedGivesEmptyList(){
    	VagvalAgent va = new VagvalAgent();
    	va.setLocalTakCache("non-existing-file.txt");
    	List<VirtualiseringsInfoType> routingInfo = null;
		routingInfo = va.getVirtualiseringsInfo();
    	assertNotNull(routingInfo);
    	assertTrue(routingInfo.isEmpty());
    }

    private VagvalAgent setupVagvalAgent(
            SokVagvalsInfoInterface takService, String localTakCache) {

        URL hsaUrl = getClass().getClassLoader().getResource("hsacache.xml");
        HsaCache hsaCache = new HsaCacheImpl().init(hsaUrl.getFile());

        VagvalAgent vagvalAgent = new VagvalAgent();
        vagvalAgent.setAddressDelimiter("#");
        vagvalAgent.setHsaCache(hsaCache);

        // Prepare agent with local cache and that call to TAK returns null
        // results as when TAK is not available.
        vagvalAgent.setLocalTakCache(localTakCache);
        vagvalAgent.setPort(takService);
        return vagvalAgent;
    }

	private XMLGregorianCalendar createTimestamp() throws Exception {
		XMLGregorianCalendar time = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);
		time.add(anHourAgo);
		return time;
	}

    private SokVagvalsInfoInterface mockedTakGivesNoResult() {
        // Mock TAK service to return no result for authorizations and
        // virtualizations
        SokVagvalsInfoInterface mockedTakService = mock(SokVagvalsInfoInterface.class);
        when(mockedTakService.hamtaAllaAnropsBehorigheter(anyObject())).thenReturn(null);
        when(mockedTakService.hamtaAllaVirtualiseringar(anyObject())).thenReturn(null);
        return mockedTakService;
    }
}
