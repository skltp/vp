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
import se.skl.tp.vp.util.MessageProperties;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

/**
 * Helper class for working with addressing
 */
public class AddressingHelper {
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

		// No routing with matching Riv-version found for
		raiseError(rivProfiles.size() == 0, VpSemanticErrorCodeEnum.VP005, input);

		// More than one route with matching Riv-version found for
		raiseError(rivProfiles.size() > 1, VpSemanticErrorCodeEnum.VP006, input);

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
		
		// No RIV version configured
		raiseError(request.rivVersion == null, VpSemanticErrorCodeEnum.VP001);
		
		// No sender ID (from_address) found in certificate
		raiseError(request.senderId == null, VpSemanticErrorCodeEnum.VP002);
		
		// No receiver ID (to_address) found in message
		raiseError(request.receiverId == null, VpSemanticErrorCodeEnum.VP003);
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

		// No Logical Adress with matching Riv-version found for ...
		if (noOfMatchingAdresses == 0) {
			String errorMessage = request.getSummary();
			raiseError(VpSemanticErrorCodeEnum.VP005, errorMessage);
		}

		// More than one Logical Adress with matching Riv-version found for ...
		if (noOfMatchingAdresses > 1) {
			String errorMessage = request.getSummary();
			raiseError(VpSemanticErrorCodeEnum.VP006, errorMessage);
		}

		// Physical Adress field is empty in Service Producer for
		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = request.getSummary();
			raiseError(VpSemanticErrorCodeEnum.VP010, errorMessage);
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

			raiseError(request.receiverId == null, VpSemanticErrorCodeEnum.VP003);

			// Check if whitespace in incoming receiverid and give a hint in the error message if found.
			String whitespaceDetectedHintString = "";
			if (request.receiverId.contains(" ")) {
				whitespaceDetectedHintString = ". Whitespace detected in incoming request!";
			}
			// No Logical Address found for serviceNamespace
			String errorMessage = request.getSummary() + ", From:" + vpInstanceId + whitespaceDetectedHintString;
			raiseError(VpSemanticErrorCodeEnum.VP004, errorMessage);
		}

		return virtualiseringar;
	}
	
	private void raiseError(boolean test, VpSemanticErrorCodeEnum codeenum, VagvalInput request) {
		if(test)
			raiseError(codeenum, request == null ? "" : request.getSummary());
	}
	
	private void raiseError(boolean test, VpSemanticErrorCodeEnum codeenum) {
		if(test)
			raiseError(codeenum, null);
	}
	
	private void raiseError(VpSemanticErrorCodeEnum codeEnum, String suffix) {
		String errmsg = MessageProperties.getInstance().get(codeEnum, suffix);
		log.error(errmsg);
		throw new VpSemanticException(errmsg, codeEnum);		
	}
}
