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

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.mule.api.annotations.expressions.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontract.v2.rivtabp21.GetLogicalAddresseesByServiceContractResponderInterface;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.FilterType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.GetLogicalAddresseesByServiceContractResponseType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.GetLogicalAddresseesByServiceContractType;
import se.rivta.infrastructure.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v2.LogicalAddresseeRecordType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.FilterInfoType;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

/**
 * GetLogicalAddressesByServiceContract by using the local cache instead of
 * calling Service Catalog (TAK) every time.
 */
@WebService(serviceName = "GetLogicalAddresseesByServiceContractResponderService", portName = "GetLogicalAddresseesByServiceContractResponderPort", name = "GetLogicalAddresseesByServiceContractInteraction")
public class GetLogicalAddresseesByServiceContract implements GetLogicalAddresseesByServiceContractResponderInterface {

	private static final Logger log = LoggerFactory.getLogger(GetLogicalAddresseesByServiceContract.class);

	@Lookup("vagvalAgent")
	private VagvalAgent vagvalAgent;

	public void setVagvalAgent(VagvalAgent vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	public GetLogicalAddresseesByServiceContractResponseType getLogicalAddresseesByServiceContract(
			String logicalAddress, GetLogicalAddresseesByServiceContractType request) {

		if (!requestIsValidAccordingToRivSpec(request)) {
			return new GetLogicalAddresseesByServiceContractResponseType();
		}

		/*
		 * This is a fix to solve the problem with TAK holding serviceinteraction namespace aand not servicecontract namespace. Because
		 * VP should not infect other systems with this problem GetLogicalAddresseesByServiceContract needs to always return the
		 * servicecontract namespace.
		 *
		 * Lets say these are registered in TAK:
		 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21
		 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp20
		 *
		 * Then this code needs to solve to only return the servicecontract namespace, which is the same for different rivtabp versions:
		 * urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1
		 */
		Map<String, LogicalAddresseeRecordType> uniqueLogicalAddresses = new HashMap<String,LogicalAddresseeRecordType>();
		for (AnropsBehorighetsInfoType authInfo : vagvalAgent.getAnropsBehorighetsInfoList()) {
			if (!contains(authInfo, uniqueLogicalAddresses) && validAccordingToTime(authInfo) && matchesRequested(authInfo, request)) {

				/*
				 * FIXME: Hur skall vi hantera om filter i TAK finns definierat på både:
				 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21
				 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp20
				 *
				 * Så länge som vi har kvar buggen med att wsdl namespace används i TAK?
				 * Det går att komma runt genom att i TAK definera samma filter på alla
				 * tjänsteinteraktioner.
				 */

				LogicalAddresseeRecordType logicalAddressee = new LogicalAddresseeRecordType();
				logicalAddressee.setLogicalAddress(authInfo.getReceiverId());

				addFilterToResponse(authInfo, logicalAddressee);

				uniqueLogicalAddresses.put(authInfo.getReceiverId(), logicalAddressee);

			}
		}

		GetLogicalAddresseesByServiceContractResponseType response = new GetLogicalAddresseesByServiceContractResponseType();
		response.getLogicalAddressRecord().addAll(uniqueLogicalAddresses.values());

		if (log.isInfoEnabled()) {
			String consumerHsaId = request.getServiceConsumerHsaId();
			String namespace = request.getServiceContractNameSpace().getServiceContractNamespace();
			log.info("getLogicalAddresseesByServiceContract.v2 found {} logical addresses for consumerHsaId: {}, namespace: {}", new Object[] {
					uniqueLogicalAddresses.size(), consumerHsaId, namespace });
		}

		return response;
	}

	/*
	 * add filter to response if any filters are defined in TAK
	 */
	private void addFilterToResponse(AnropsBehorighetsInfoType authInfo,
			LogicalAddresseeRecordType logicalAddressee) {

		if(authInfo == null || authInfo.getFilterInfo() == null){
			return;
		}

		for (FilterInfoType filterInfoType : authInfo.getFilterInfo()) {
			FilterType filter = new FilterType();
			filter.setServiceDomain(filterInfoType.getServiceDomain());
			filter.getCategorization().addAll(filterInfoType.getCategorization());
			logicalAddressee.getFilter().add(filter);
		}
	}

	static boolean contains(AnropsBehorighetsInfoType authInfo,
			Map<String, LogicalAddresseeRecordType> uniqueLogicalAddresses) {
		return uniqueLogicalAddresses.containsKey(authInfo.getReceiverId());
	}

	static boolean requestIsValidAccordingToRivSpec(GetLogicalAddresseesByServiceContractType request) {
		return request.getServiceConsumerHsaId() != null && request.getServiceContractNameSpace() != null
				&& request.getServiceContractNameSpace().getServiceContractNamespace() != null;
	}

	/*
	 * Compare the requested namespace with the one stored in TAK. Should handle both case when service interaction
	 * namespace(wsdl) is in the request and a correct service contract namespace is in the request.
	 */
	private boolean matchesRequested(AnropsBehorighetsInfoType authInfo,
			GetLogicalAddresseesByServiceContractType request) {
		String namespace = extractFirstPartOfNamespace(request.getServiceContractNameSpace()
				.getServiceContractNamespace());
		return authInfo.getSenderId().equals(request.getServiceConsumerHsaId())
				&& authInfo.getTjansteKontrakt().startsWith(namespace);
	}

	/*
	 * Split the first part of a namespace to be able to compare with the service interaction namespace (wsdl)
	 * declared in TAK.
	 *
	 * urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1
	 * should return
	 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule
	 *
	 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21
	 * should return
	 * urn:riv:crm:scheduling:GetSubjectOfCareSchedule:1:rivtabp21
	 */
	static String extractFirstPartOfNamespace(String namespace) {
		return namespace.split("Responder")[0];
	}

	private boolean validAccordingToTime(AnropsBehorighetsInfoType authInfo) {
		XMLGregorianCalendar requestTime = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		return requestTime.compare(authInfo.getFromTidpunkt()) != DatatypeConstants.LESSER
				&& requestTime.compare(authInfo.getTomTidpunkt()) != DatatypeConstants.GREATER;
	}

}
