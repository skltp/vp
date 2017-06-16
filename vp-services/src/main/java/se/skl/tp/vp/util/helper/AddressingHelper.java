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
package se.skl.tp.vp.util.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

/**
 * Helper class for working with addressing
 */
public class AddressingHelper {
	private static final String RIVTA_VERSION_PATTERN = "^rivtabp\\d{2}$";

	private static final Logger log = LoggerFactory.getLogger(AddressingHelper.class);

	private VisaVagvalsInterface agent;
	private String vpInstanceId;

	public AddressingHelper(final VisaVagvalsInterface agent, String vpInstanceId) {
		this.agent = agent;
		this.vpInstanceId = vpInstanceId;
	}

	public String getAvailableRivProfile(MuleMessage muleMessage) {

		final VagvalInput input = this.createRequestToServiceDirectory(muleMessage);
		final VisaVagvalResponse response = agent.visaVagval(this.createVisaVagvalRequest(input));

		final List<VirtualiseringsInfoType> virts = this.getAllVirtualizedServices(response, input);

		final Set<String> rivProfiles = new HashSet<String>();
		for (final VirtualiseringsInfoType virt : virts) {
			rivProfiles.add(virt.getRivProfil());
		}

		if (rivProfiles.size() == 0) {
			String errorMessage = VpSemanticErrorCodeEnum.VP005 + " No ruting with matching Riv-version found for serviceContractNamespace :"
					+ input.serviceContractNamespace + ", receiverId:" + input.receiverId + "RivVersion" + input.rivVersion;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP005);
		}

		if (rivProfiles.size() > 1) {
			String errorMessage = VpSemanticErrorCodeEnum.VP006 + " More than one route with matching Riv-version found for serviceContractNamespace:"
					+ input.serviceContractNamespace + ", receiverId:" + input.receiverId;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP006);
		}

		return rivProfiles.iterator().next();
	}

	public String getAddress(MuleMessage muleMessage) {
		/*
		 * Create parameters from message
		 */
		final VagvalInput input = this.createRequestToServiceDirectory(muleMessage);
		return this.getAddressFromAgent(input);
	}

	public String getAddressFromAgent(final VagvalInput input) {
		/*
		 * Validate the parameters
		 */
		this.validateRequest(input);

		/*
		 * Create a real request from the parameters
		 */
		final VisaVagvalRequest request = this.createVisaVagvalRequest(input);

		/*
		 * Get virtualized services
		 */
		final List<VirtualiseringsInfoType> services = this.getAllVirtualizedServices(this.agent.visaVagval(request),
				input);

		/*
		 * Grab the address
		 */
		return this.getAddressToVirtualService(services, input);
	}

	private VagvalInput createRequestToServiceDirectory(MuleMessage muleMessage) {

		VagvalInput vagvalInput = new VagvalInput();
		vagvalInput.senderId = (String) muleMessage.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);
		vagvalInput.receiverId = (String) muleMessage.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION);
		vagvalInput.rivVersion = (String) muleMessage.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);
		vagvalInput.serviceContractNamespace = (String) muleMessage.getProperty(VPUtil.SERVICECONTRACT_NAMESPACE, PropertyScope.SESSION);

		return vagvalInput;
	}

	private void validateRequest(final VagvalInput request) {
		if (log.isDebugEnabled()) {
			log.debug(
					"Calling vagvalAgent with serviceNamespace:" + request.serviceContractNamespace + ", receiverId:"
							+ request.receiverId + ", senderId: " + request.senderId);
		}
		if (request.rivVersion == null) {
			String errorMessage = VpSemanticErrorCodeEnum.VP001 + " No RIV version configured";
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP001);
		}

		if(!request.rivVersion.toLowerCase().matches(RIVTA_VERSION_PATTERN)){
				String errorMessage = VpSemanticErrorCodeEnum.VP001 + " RIV-version " + request.rivVersion +" matchar inte godkänd namnstandard";
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP001);
		}

		if (request.senderId == null) {
			String errorMessage = VpSemanticErrorCodeEnum.VP002 + " No sender ID (from_address) found in certificate";
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP002);
		}
		if (request.receiverId == null) {
			String errorMessage = VpSemanticErrorCodeEnum.VP003 + " No receiver ID (to_address) found in message";
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP003);
		}
	}

	private VisaVagvalRequest createVisaVagvalRequest(final VagvalInput input) {
		return this.createVisaVagvalRequest(input.senderId, input.receiverId, input.serviceContractNamespace);
	}

	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId, String tjansteGranssnitt) {
		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);

		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		vvR.setTidpunkt(tidPunkt);

		return vvR;
	}

	private String getAddressToVirtualService(final List<VirtualiseringsInfoType> services, final VagvalInput request) {
		String adress = null;
		int noOfMatchingAdresses = 0;
		for (VirtualiseringsInfoType vvInfo : services) {
			if (vvInfo.getRivProfil().equals(request.rivVersion)) {
				adress = vvInfo.getAdress();
				noOfMatchingAdresses++;
			}
		}

		if (noOfMatchingAdresses == 0) {
			String errorMessage = VpSemanticErrorCodeEnum.VP005 + " No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP005);
		}

		if (noOfMatchingAdresses > 1) {
			String errorMessage = VpSemanticErrorCodeEnum.VP006 + " More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP006);
		}

		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = VpSemanticErrorCodeEnum.VP010 + " Physical Adress field is empty in Service Producer for serviceNamespace :"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP010);
		}

		return adress;
	}

	private List<VirtualiseringsInfoType> getAllVirtualizedServices(final VisaVagvalResponse response,
			final VagvalInput request) {
		List<VirtualiseringsInfoType> virtualiseringar = response.getVirtualiseringsInfo();
		if (log.isDebugEnabled()) {
			log.debug("VagvalAgent response count: " + virtualiseringar.size());
			for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
				log.debug(
						"VagvalAgent response item RivProfil: " + vvInfo.getRivProfil() + ", Address: "
								+ vvInfo.getAdress());
			}
		}

		if (virtualiseringar.size() == 0) {
			if (request.receiverId == null) {
				String errorMessage = VpSemanticErrorCodeEnum.VP003 + " No receiver ID (to_address) found in message";
				log.error(errorMessage);
				throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP003);
			}

			// Check if whitespace in incoming receiverid and give a hint in the error message if found.
			String whitespaceDetectedHintString = "";
			if (request.receiverId.contains(" ")) {
				whitespaceDetectedHintString = ". Whitespace detected in incoming request!";
			}
			String errorMessage = VpSemanticErrorCodeEnum.VP004 + " No Logical Adress found for serviceNamespace:" + request.serviceContractNamespace
					+ ", receiverId:" + request.receiverId + ", From:" + vpInstanceId + whitespaceDetectedHintString;
			log.info(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP004);
		}

		return virtualiseringar;
	}
}
