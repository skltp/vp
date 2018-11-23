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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
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
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCacheLog;
import se.skltp.takcache.TakCacheLog.RefreshStatus;
import se.skltp.takcache.exceptions.TakServiceException;
import se.skltp.takcache.services.TakService;

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

		vagvalAgent = new VagvalAgentMock(hsaCache,"#");
		vagvalAgent.setLocalCacheFileName(localTakCache.getAbsolutePath());
	}

	@Test
	public void nationalRoutingAndAuthorizationNationalConsumer() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.size());
		assertEquals("https://SE", response.get(0).getAddress());
		assertEquals(RIVTABP21, response.get(0).getRivProfile());
	}

	@Test
	public void unknownRecieverGivesRoutingToSEWhenAuthorizationExist() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));


		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(UNKNOWN_HEALTHCAREPROVIDER);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.size());
		assertEquals("https://SE", response.get(0).getAddress());
		assertEquals(RIVTABP21, response.get(0).getRivProfile());

	}

	@Test
	public void unknownRecieverGivesNoRoutingToSEWhenAuthorizationDontExist() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));


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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));


		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.size());
		assertEquals("https://providerA", response.get(0).getAddress());
		assertEquals(RIVTABP21, response.get(0).getRivProfile());
	}

	@Test
	public void nationalRoutingLocalAuthorizationLocalConsumer() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_A);
		request.setSenderId(LOCAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.size());
		assertEquals("https://SE", response.get(0).getAddress());
		assertEquals(RIVTABP21, response.get(0).getRivProfile());
	}

	@Test
	public void localConsumerWithNoAccessToLocalHealthCareProvider() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerB", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerB", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_B));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(LOCAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREPROVIDER_A);
		request.setSenderId(LOCAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals(1, response.size());
		assertEquals("https://providerA", response.get(0).getAddress());
		assertEquals(RIVTABP21, response.get(0).getRivProfile());
	}

	@Test
	public void deprecatedDefaultRoutingDontUseHsaTreeForRouting() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

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

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);
		assertTrue(response.isEmpty());

	}

	@Test
	public void deprecatedDefaultRoutingDontUseHsaTreeForAuthorization() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_SCHEDULING, SE));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://providerA", RIVTABP21, CRM_SCHEDULING, HEALTHCAREUNIT_A));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, HEALTHCAREPROVIDER_A));

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
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER, CRM_SCHEDULING, SE));


		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId(HEALTHCAREUNIT_A);
		request.setSenderId(NATIONAL_CONSUMER);
		request.setTidpunkt(createTimestamp());
		request.setTjanstegranssnitt(CRM_SCHEDULING);

		List<RoutingInfo> response = vagvalAgent.visaVagval(request);

		assertEquals("No routing found", 0, response.size());
	}

    @Test
    public void noContactWithTjanstekatalogenAlwaysReultsInLocalCacheIsRead()
            throws Exception {

        TakService mockedTakService = mockedTakServiceGivesNoResult();
        URL localTakCache = getClass().getClassLoader().getResource("tklocalcache-test.xml");
        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, localTakCache.getFile());
        TakCacheLog takCacheLog = vagvalAgent.resetVagvalCache();

				assertEquals(1, takCacheLog.getNumberBehorigheter());
				assertEquals(RefreshStatus.RESTORED_FROM_LOCAL_CACHE, takCacheLog.getRefreshStatus());

			// Verify authorizations are loaded from local cache
        List<AnropsBehorighetsInfoType> authorization = vagvalAgent
                .getAnropsBehorighetsInfoList();

        assertEquals(1, authorization.size());
        assertEquals("vp-test-producer", authorization.get(0).getReceiverId());
        assertEquals("tp", authorization.get(0).getSenderId());
        assertEquals("urn:riv:domain:subdomain:GetProductDetailResponder:1", authorization.get(0)
                .getTjansteKontrakt());


        assertEquals(1, takCacheLog.getNumberVagval());
        assertEquals(RefreshStatus.RESTORED_FROM_LOCAL_CACHE, takCacheLog.getRefreshStatus());

//        assertEquals("vp-test-producer", virtualizations.get(0).getReceiverId());
//        assertEquals("https://localhost:19000/vardgivare-b/tjanst1",
//                virtualizations.get(0).getAdress());
//        assertEquals("RIVTABP20", virtualizations.get(0).getRivProfil());
//        assertEquals("urn:riv:domain:subdomain:GetProductDetailResponder:1", virtualizations.get(0)
//                .getTjansteKontrakt());

    }



    @Test
    public void contactWithTjanstekatalogenAlwaysReultsInLocalCacheIsUpdated()
            throws Exception {

        File localTakCache = folder.newFile(".tk.empty.localCache");
				vagvalAgent.setLocalCacheFileName(localTakCache.getAbsolutePath());

        // Prepare result from TAK that will update the local cache
				vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

				vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER,
                CRM_SCHEDULING, SE));

        vagvalAgent.resetVagvalCache();

        XMLAssert.assertXpathExists("/persistentCache/virtualiseringsInfo", new InputSource(new FileReader(localTakCache)));
        XMLAssert.assertXpathExists("/persistentCache/anropsBehorighetsInfo",new InputSource(new FileReader(localTakCache)));
    }

    @Test
    public void contactWithTjanstekatalogenDuringInitIsReportedInProcessingLog()
            throws Exception {

       File localTakCache = folder.newFile(".tk.empty.localCache");
				vagvalAgent.setLocalCacheFileName(localTakCache.getAbsolutePath());


        // Prepare result from TAK that will update the local cache
        vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://SE", RIVTABP21, CRM_LISTING, SE));

				vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization(NATIONAL_CONSUMER,
                CRM_SCHEDULING, SE));


        // Init VagvalAgent from local cache
        TakCacheLog processingLog = vagvalAgent.resetVagvalCache();

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("Host:"));
        assertThat(log.get(1), CoreMatchers.containsString("Time:"));
        assertThat(log.get(2), CoreMatchers.containsString("Initialize TAK cache resources.."));
        assertThat(log.get(3), CoreMatchers.containsString("Succeeded to get virtualizations and/or permissions from TAK, save to local TAK copy..."));
        assertThat(log.get(4), CoreMatchers.containsString("Succesfully saved virtualizations and permissions to local TAK copy:"));
        assertThat(log.get(5), CoreMatchers.containsString("Init TAK cache loaded number of permissions: 1"));
        assertThat(log.get(6), CoreMatchers.containsString("Init TAK cache loaded number of virtualizations: 1"));
        assertThat(log.get(7), CoreMatchers.containsString("Init done, was successful: true"));
    }

    @Test
    public void noContactWithTjanstekatalogenDuringInitIsReportedInProcessingLog()
            throws Exception {

        TakService mockedTakService = mockedTakServiceGivesNoResult();

        // The local cache to read when TAK is not available
        URL localTakCache = getClass().getClassLoader().getResource("tklocalcache-test.xml");

        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, localTakCache.getFile());

        // Init VagvalAgent from local cache
        TakCacheLog processingLog = vagvalAgent.resetVagvalCache();

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("Host:"));
        assertThat(log.get(1), CoreMatchers.containsString("Time:"));
        assertThat(log.get(2), CoreMatchers.containsString("Initialize TAK cache resources.."));
        assertThat(log.get(3), CoreMatchers.containsString("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy..."));
        assertThat(log.get(4), CoreMatchers.containsString("Succesfully restored virtualizations and permissions from local TAK copy:"));
        assertThat(log.get(5), CoreMatchers.containsString("Init TAK cache loaded number of permissions: 1"));
        assertThat(log.get(6), CoreMatchers.containsString("Init TAK cache loaded number of virtualizations: 1"));
        assertThat(log.get(7), CoreMatchers.containsString("Init done, was successful: true"));
    }

    @Test
    public void corruptLocalTakCacheDuringRestoreIsReportedInProcessongLog()
            throws Exception {

        TakService mockedTakService = mockedTakServiceGivesNoResult();

        // A corrupt local TAK cache
        File corruptLocalTakCache = folder.newFile(".tk.corrupt.localCache");

        VagvalAgent vagvalAgent = setupVagvalAgent(mockedTakService, corruptLocalTakCache.getAbsolutePath());

        // Init VagvalAgent from local cache
        TakCacheLog processingLog = vagvalAgent.resetVagvalCache();

        List<String> log = processingLog.getLog();
        assertThat(log.get(0), CoreMatchers.containsString("Host:"));
        assertThat(log.get(1), CoreMatchers.containsString("Time:"));
        assertThat(log.get(2), CoreMatchers.containsString("Initialize TAK cache resources.."));
        assertThat(log.get(3), CoreMatchers.containsString("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy..."));
        assertThat(log.get(4), CoreMatchers.containsString("Failed to restore virtualizations and permissions from local TAK copy:"));
        assertThat(log.get(5), CoreMatchers.containsString("Reason for failure: javax.xml.bind.UnmarshalException"));
    }

    @Test
    public void noAuthorizationsLoadedGivesEmptyList(){
    	VagvalAgentMock va = new VagvalAgentMock();
    	va.setLocalCacheFileName("non-existing-file.txt");
    	List<AnropsBehorighetsInfoType> authInfo = va.getAnropsBehorighetsInfoList();
    	assertNotNull(authInfo);
    	assertTrue(authInfo.isEmpty());
    }

    @Test
    public void noRoutingLoadedGivesEmptyList(){
    	VagvalAgentMock va = new VagvalAgentMock();
			va.setLocalCacheFileName("non-existing-file.txt");
			TakCacheLog takCacheLog  = va.resetVagvalCache();

    	assertEquals(0, takCacheLog.getNumberVagval());
    	assertEquals(RefreshStatus.REFRESH_FAILED, takCacheLog.getRefreshStatus());
    }

    private VagvalAgent setupVagvalAgent(TakService takService, String localTakCache) {

        URL hsaUrl = getClass().getClassLoader().getResource("hsacache.xml");
        HsaCache hsaCache = new HsaCacheImpl().init(hsaUrl.getFile());

        VagvalAgentMock vagvalAgent = new VagvalAgentMock(takService, hsaCache, "#");
        vagvalAgent.setLocalCacheFileName(localTakCache);
        return vagvalAgent;
    }

	private XMLGregorianCalendar createTimestamp() throws Exception {
		XMLGregorianCalendar time = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);
		time.add(anHourAgo);
		return time;
	}

    private TakService mockedTakServiceGivesNoResult() throws TakServiceException {
        // Mock TAK service to return no result for authorizations and
        // virtualizations
        TakService mockedTakService = mock(TakService.class);
        when(mockedTakService.getBehorigheter()).thenReturn(null);
        when(mockedTakService.getVirtualiseringar()).thenReturn(null);
        return mockedTakService;
    }
}
