package se.skl.tp.vp.getlogicaladdresseesbyservicecontract;

import java.util.HashSet;
import java.util.Set;

import javax.jws.WebService;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.mule.api.annotations.expressions.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.riv.itintegration.registry.getlogicaladdresseesbyservicecontract.v1.rivtabp21.GetLogicalAddresseesByServiceContractResponderInterface;
import se.riv.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v1.GetLogicalAddresseesByServiceContractResponseType;
import se.riv.itintegration.registry.getlogicaladdresseesbyservicecontractresponder.v1.GetLogicalAddresseesByServiceContractType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

/**
 * GetLogicalAddressesByServiceContract by using the local cache instead of
 * calling Service Catalog every time.
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

		Set<String> uniqueLogicalAddresses = new HashSet<String>();
		for (AnropsBehorighetsInfoType authInfo : vagvalAgent.getAnropsBehorighetsInfoList()) {
			if (validAccordingToTime(authInfo) && matchesRequested(authInfo, request)) {
				uniqueLogicalAddresses.add(authInfo.getReceiverId());
			}
		}

		GetLogicalAddresseesByServiceContractResponseType response = new GetLogicalAddresseesByServiceContractResponseType();
		response.getLogicalAddress().addAll(uniqueLogicalAddresses);

		if (log.isDebugEnabled()) {
			String consumerHsaId = request.getServiceConsumerHsaId();
			String namespace = request.getServiceContractNameSpace().getServiceContractNamespace();
			log.debug("Found {} logical addresses for consumerHsaId: {}, namespace: {}", new Object[] {
					uniqueLogicalAddresses.size(), consumerHsaId, namespace });
		}

		return response;
	}

	static boolean requestIsValidAccordingToRivSpec(GetLogicalAddresseesByServiceContractType request) {
		return request.getServiceConsumerHsaId() != null && request.getServiceContractNameSpace() != null
				&& request.getServiceContractNameSpace().getServiceContractNamespace() != null;
	}

	private boolean matchesRequested(AnropsBehorighetsInfoType authInfo,
			GetLogicalAddresseesByServiceContractType request) {
		String namespace = extractFirstPartOfNamespace(request.getServiceContractNameSpace()
				.getServiceContractNamespace());
		return authInfo.getSenderId().equals(request.getServiceConsumerHsaId())
				&& authInfo.getTjansteKontrakt().startsWith(namespace);
	}

	private boolean validAccordingToTime(AnropsBehorighetsInfoType authInfo) {
		XMLGregorianCalendar requestTime = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		return requestTime.compare(authInfo.getFromTidpunkt()) != DatatypeConstants.LESSER
				&& requestTime.compare(authInfo.getTomTidpunkt()) != DatatypeConstants.GREATER;
	}

	static String extractFirstPartOfNamespace(String namespace) {
		return namespace.split("Responder")[0];
	}

}
