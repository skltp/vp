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
package se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v2.GetLogicalAddresseesByServiceContract.requestIsValidAccordingToRivSpec;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.FilterType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.GetLogicalAddresseesByServiceContractResponseType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.GetLogicalAddresseesByServiceContractType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.LogicalAddresseeRecordType;
import se.rivta.infrastructure.itintegration.registry.v2.ServiceContractNamespaceType;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalagent.VagvalAgent;
import se.skl.tp.vp.vagvalagent.VagvalAgentMock;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoIdType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.FilterInfoType;

public class GetLogicalAddresseesByServiceContractTest {

	private static final String CONSUMER_HSAID_1 = "SENDERID-1";
	private static final String RECEIVERID_1 = "RECEIVERID-1";
	private static final String RECEIVERID_2 = "RECEIVERID-2";

	@Test
	public void usingServiceContractWhenSearchingAmongServiceContracts() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());

		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1", CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);

		assertEquals(2, response.getLogicalAddressRecord().size());
		
		String lar1 = response.getLogicalAddressRecord().get(0).getLogicalAddress();
		String lar2 = response.getLogicalAddressRecord().get(1).getLogicalAddress();
		assertTrue(RECEIVERID_1.equals(lar1) && RECEIVERID_2.equals(lar2)
				|| RECEIVERID_1.equals(lar2) && RECEIVERID_2.equals(lar1));		
	}

	@Test
	public void senderDoesNotHaveAnyAuthorizedLogicalAddressesForServiceContract() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());

		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1", "NONE");
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);

		assertEquals(0, response.getLogicalAddressRecord().size());
	}

	@Test
	public void oldServiceContractAuthorizationsShouldNotBeReturned() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());

		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:itintegration:monitoring:PingForConfigurationResponder:1",CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);

		assertEquals(0, response.getLogicalAddressRecord().size());
	}

	@Test
	public void requestIsInValidAccordingToRivSpec() {
		GetLogicalAddresseesByServiceContractType requestWithoutMandatoryElements = new GetLogicalAddresseesByServiceContractType();
		assertFalse(requestIsValidAccordingToRivSpec(requestWithoutMandatoryElements));
	}

	@Test
	public void filtersDefinedInSeveralDomains() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());

		GetLogicalAddresseesByServiceContractType requestCrmScheduling = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1", CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType responseCrmScheduling = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", requestCrmScheduling);

		GetLogicalAddresseesByServiceContractType requestEngagementindex = createRequest("urn:riv:itintegration.engagementindex:FindContentResponder:1", CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType responseEngagementindex = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", requestEngagementindex);

		assertEquals(2, responseCrmScheduling.getLogicalAddressRecord().size());
		assertEquals(1, responseEngagementindex.getLogicalAddressRecord().size());
		
		// fix order of records for easy comparison in asserts below
		{
			Comparator<LogicalAddresseeRecordType> c = (o1, o2) -> o1.getLogicalAddress().compareTo(o2.getLogicalAddress());
			responseCrmScheduling.getLogicalAddressRecord().sort(c);
		}

		//Receiver RECEIVERID-1 crm:scheduling
		assertEquals(RECEIVERID_1, responseCrmScheduling.getLogicalAddressRecord().get(0).getLogicalAddress());
		FilterType filterReceiver1CrmScheduling = responseCrmScheduling.getLogicalAddressRecord().get(0).getFilter().get(0);
		assertEquals("crm:scheduling", filterReceiver1CrmScheduling.getServiceDomain());
		assertEquals(2, filterReceiver1CrmScheduling.getCategorization().size());
		assertEquals("Booking", filterReceiver1CrmScheduling.getCategorization().get(0));
		assertEquals("Invitation", filterReceiver1CrmScheduling.getCategorization().get(1));

		//Receiver RECEIVERID-2 itintegration:engegaementindex
		assertEquals(RECEIVERID_2, responseEngagementindex.getLogicalAddressRecord().get(0).getLogicalAddress());
		FilterType filterReceiver1Engagementindex = responseEngagementindex.getLogicalAddressRecord().get(0).getFilter().get(0);
		assertEquals("itintegration:engagementindex", filterReceiver1Engagementindex.getServiceDomain());
		assertEquals(1, filterReceiver1Engagementindex.getCategorization().size());
		assertEquals("Update", filterReceiver1Engagementindex.getCategorization().get(0));

		//Receiver RECEIVERID-2 crm:scheduling
		assertEquals(RECEIVERID_2, responseCrmScheduling.getLogicalAddressRecord().get(1).getLogicalAddress());
		FilterType filterReceiver2 = responseCrmScheduling.getLogicalAddressRecord().get(1).getFilter().get(0);
		assertEquals("crm:scheduling", filterReceiver2.getServiceDomain());
		assertEquals(1, filterReceiver2.getCategorization().size());
		assertEquals("Invitation", filterReceiver2.getCategorization().get(0));
	}

	private GetLogicalAddresseesByServiceContractType createRequest(String nameSpace, String consumer) {
		GetLogicalAddresseesByServiceContractType request =	new GetLogicalAddresseesByServiceContractType();
		request.setServiceConsumerHsaId(consumer);
		request.setServiceContractNameSpace(serviceContract(nameSpace));
		return request;
	}

	private ServiceContractNamespaceType serviceContract(String serviceContractNamespace ) {
		ServiceContractNamespaceType contractNamespaceType = new ServiceContractNamespaceType();
		contractNamespaceType.setServiceContractNamespace(serviceContractNamespace);
		return contractNamespaceType;
	}

	private VagvalAgent createVagvalAgentContainingServiceContractNamespaces() {

		VagvalAgentMock vagvalAgentMock = new VagvalAgentMock(null, "#");
		List<AnropsBehorighetsInfoType> anropsBehorighetsInfo = vagvalAgentMock.getMockAnropsBehorighetsInfo();
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1",createFilter("crm:scheduling", "Booking", "Invitation")));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_2,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1",createFilter("crm:scheduling", "Invitation")));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_2,CONSUMER_HSAID_1,"urn:riv:itintegration.engagementindex:FindContentResponder:1",createFilter("itintegration:engagementindex", "Update")));
		anropsBehorighetsInfo.add(oldAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:itintegration:monitoring:PingForConfigurationResponder:1"));

		vagvalAgentMock.setUseVagvalCache(false);
		return vagvalAgentMock;
	}

	private AnropsBehorighetsInfoType validAuthorization(String receiverId, String senderId, String tjansteKontrakt, FilterInfoType... filter) {
		AnropsBehorighetsInfoType behorighetsInfoType = new AnropsBehorighetsInfoType();
		behorighetsInfoType.setReceiverId(receiverId);
		behorighetsInfoType.setSenderId(senderId);
		behorighetsInfoType.setTjansteKontrakt(tjansteKontrakt);

		Calendar calAWeekAgo = Calendar.getInstance ();
		calAWeekAgo.add(Calendar.WEEK_OF_YEAR, -1);
		Date aWeekAgo = calAWeekAgo.getTime ();

		Calendar calAWeekAhead = Calendar.getInstance ();
		calAWeekAhead.add(Calendar.WEEK_OF_YEAR, 1);
		Date aWeekAhead = calAWeekAhead.getTime ();

		behorighetsInfoType.setTomTidpunkt(XmlGregorianCalendarUtil.fromDate(aWeekAhead));
		behorighetsInfoType.setFromTidpunkt(XmlGregorianCalendarUtil.fromDate(aWeekAgo));
		behorighetsInfoType.setAnropsBehorighetsInfoId(new AnropsBehorighetsInfoIdType());

		//Filters and categories
		behorighetsInfoType.getFilterInfo().addAll(Arrays.asList(filter));

		return behorighetsInfoType;
	}

	private FilterInfoType createFilter(String serviceDomain, String... cetegorizations) {
		FilterInfoType filter = new FilterInfoType();
		filter.setServiceDomain(serviceDomain);
		filter.getCategorization().addAll(Arrays.asList(cetegorizations));
		return filter;
	}

	private AnropsBehorighetsInfoType oldAuthorization(String receiverId, String senderId, String tjansteKontrakt) {
		AnropsBehorighetsInfoType behorighetsInfoType = new AnropsBehorighetsInfoType();
		behorighetsInfoType.setReceiverId(receiverId);
		behorighetsInfoType.setSenderId(senderId);
		behorighetsInfoType.setTjansteKontrakt(tjansteKontrakt);

		Calendar oldStartDateCal = Calendar.getInstance();
		oldStartDateCal.set(2000, Calendar.OCTOBER, 1);
		Date oldStartDate = oldStartDateCal.getTime();

		Calendar oldEndDateCal = Calendar.getInstance();
		oldEndDateCal.set(2000, Calendar.OCTOBER, 2);
		Date oldEndDate = oldEndDateCal.getTime();

		behorighetsInfoType.setTomTidpunkt(XmlGregorianCalendarUtil.fromDate(oldStartDate));
		behorighetsInfoType.setFromTidpunkt(XmlGregorianCalendarUtil.fromDate(oldEndDate));
		behorighetsInfoType.setAnropsBehorighetsInfoId(new AnropsBehorighetsInfoIdType());

		return behorighetsInfoType;
	}

}
