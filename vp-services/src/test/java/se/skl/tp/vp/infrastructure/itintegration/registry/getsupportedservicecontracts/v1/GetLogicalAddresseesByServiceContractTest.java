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
package se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v1.GetLogicalAddresseesByServiceContract.extractFirstPartOfNamespace;
import static se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v1.GetLogicalAddresseesByServiceContract.requestIsValidAccordingToRivSpec;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import se.rivta.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v1.GetLogicalAddresseesByServiceContractResponseType;
import se.rivta.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v1.GetLogicalAddresseesByServiceContractType;
import se.rivta.itintegration.registry.v1.ServiceContractNamespaceType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalagent.VagvalAgent;
import se.skl.tp.vp.vagvalagent.VagvalAgentMock;

/**
 * @deprecated  As of release 2.2.2, replaced by {@link se.skl.tp.vp.infrastructure.itintegration.registry.getsupportedservicecontracts.v2.GetLogicalAddresseesByServiceContractTest}
 */
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
		
		assertEquals(2, response.getLogicalAddress().size());
		assertEquals(RECEIVERID_1, response.getLogicalAddress().get(0));
		assertEquals(RECEIVERID_2, response.getLogicalAddress().get(1));
	}
	
	@Test
	public void usingServiceContractWhenSearchingAmongServiceInteractions() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceInteractionNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1",CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(2, response.getLogicalAddress().size());
		assertEquals(RECEIVERID_1, response.getLogicalAddress().get(0));
		assertEquals(RECEIVERID_2, response.getLogicalAddress().get(1));
	}
	
	@Test
	public void usingServiceInteractionWhenSearchingAmongServiceInteractions() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceInteractionNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21",CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(2, response.getLogicalAddress().size());
		assertEquals(RECEIVERID_1, response.getLogicalAddress().get(0));
		assertEquals(RECEIVERID_2, response.getLogicalAddress().get(1));
	}
	
	@Test
	public void senderDoesNotHaveAnyAuthorizedLogicalAddressesForServiceContract() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1", "NONE");
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(0, response.getLogicalAddress().size());
	}
	
	@Test
	public void senderDoesNotHaveAnyAuthorizedLogicalAddressesForServiceInteraction() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceInteractionNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21", "NONE");
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(0, response.getLogicalAddress().size());
	}
	
	@Test
	public void oldServiceContractAuthorizationsShouldNotBeReturned() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceContractNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:itintegration:monitoring:PingForConfigurationResponder:1",CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(0, response.getLogicalAddress().size());
	}
	
	@Test
	public void oldServiceInteractionAuthorizationsShouldNotBeReturned() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceInteractionNamespaces());
		
		GetLogicalAddresseesByServiceContractType request = createRequest("urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21",CONSUMER_HSAID_1);
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", request);
		
		assertEquals(0, response.getLogicalAddress().size());
	}
	
	@Test
	public void requestWithoutMandatoryRivElementsShouldReturnEmptyResult() {
		GetLogicalAddresseesByServiceContract service = new GetLogicalAddresseesByServiceContract();
		service.setVagvalAgent(createVagvalAgentContainingServiceInteractionNamespaces());
		
		GetLogicalAddresseesByServiceContractType requestWithoutMandatoryElements = new GetLogicalAddresseesByServiceContractType();
		GetLogicalAddresseesByServiceContractResponseType response = service.getLogicalAddresseesByServiceContract("LOGICALADDRESS", requestWithoutMandatoryElements);
		
		assertEquals(0, response.getLogicalAddress().size());
	}
	
	@Test
	public void requestIsInValidAccordingToRivSpec() {
		GetLogicalAddresseesByServiceContractType requestWithoutMandatoryElements = new GetLogicalAddresseesByServiceContractType();
		assertFalse(requestIsValidAccordingToRivSpec(requestWithoutMandatoryElements));	
	}

	@Test
	public void extractFirstPartOfNamespaceWhenResponderIsIncluded() {
		String partOfNamespace = extractFirstPartOfNamespace("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1");
		assertEquals("urn:riv:crm:scheduling:GetSubjectOfCareSchedule",partOfNamespace);
	}
	
	@Test
	public void extractFirstPartOfNamespaceWhenResponderIsNotIncluded() {
		String partOfNamespace = extractFirstPartOfNamespace("urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21");
		assertEquals("urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21",partOfNamespace);
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

		List<AnropsBehorighetsInfoType> anropsBehorighetsInfo = new ArrayList<AnropsBehorighetsInfoType>();
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_2,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		anropsBehorighetsInfo.add(oldAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:itintegration:monitoring:PingForConfigurationResponder:1"));

		return new VagvalAgentMock(anropsBehorighetsInfo);
	}
	
	private VagvalAgent createVagvalAgentContainingServiceInteractionNamespaces() {

		List<AnropsBehorighetsInfoType> anropsBehorighetsInfo = new ArrayList<AnropsBehorighetsInfoType>();
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21"));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp20"));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_2,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21"));
		anropsBehorighetsInfo.add(validAuthorization(RECEIVERID_2,CONSUMER_HSAID_1,"urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp20"));
		anropsBehorighetsInfo.add(oldAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21"));
		anropsBehorighetsInfo.add(oldAuthorization(RECEIVERID_1,CONSUMER_HSAID_1,"urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp20"));	

		return new VagvalAgentMock(anropsBehorighetsInfo);
	}

	private AnropsBehorighetsInfoType validAuthorization(String receiverId, String senderId, String tjansteKontrakt) {
		AnropsBehorighetsInfoType behorighetsInfoType = new AnropsBehorighetsInfoType();
		behorighetsInfoType.setReceiverId(receiverId);
		behorighetsInfoType.setSenderId(senderId);
		behorighetsInfoType.setTjansteKontrakt(tjansteKontrakt);
		
		Calendar calAWeekAgo = Calendar.getInstance ();
		calAWeekAgo.roll(Calendar.WEEK_OF_YEAR, false);
		Date aWeekAgo = calAWeekAgo.getTime ();
		
		Calendar calAWeekAhead = Calendar.getInstance ();
		calAWeekAhead.roll(Calendar.WEEK_OF_YEAR, true);
		Date aWeekAhead = calAWeekAhead.getTime ();
		
		behorighetsInfoType.setTomTidpunkt(XmlGregorianCalendarUtil.fromDate(aWeekAhead));
		behorighetsInfoType.setFromTidpunkt(XmlGregorianCalendarUtil.fromDate(aWeekAgo));
		behorighetsInfoType.setAnropsBehorighetsInfoId(new AnropsBehorighetsInfoIdType());

		return behorighetsInfoType;
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
