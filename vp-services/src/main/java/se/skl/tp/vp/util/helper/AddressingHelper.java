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

import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.vagvalagent.VagvalAgentInterface;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.MessageProperties;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInput;
import se.skltp.takcache.RoutingInfo;

/**
 * Helper class for working with addressing
 */
public class AddressingHelper {
	private static final Logger log = LoggerFactory.getLogger(AddressingHelper.class);

	private VagvalAgentInterface agent;
	private String vpInstanceId;

	public AddressingHelper(final VagvalAgentInterface vagvalAgent, String vpInstanceId	) {
		this.agent = vagvalAgent;
		this.vpInstanceId = vpInstanceId;
	}

	public String getAvailableRivProfile(MuleMessage muleMessage) {

		final VagvalInput input = this.createRequestToServiceDirectory(muleMessage);

		final List<RoutingInfo> routingInfos = agent.visaVagval(this.createVisaVagvalRequest(input));
		if (routingInfos.isEmpty()) {
			raiseError(input.receiverId == null, VpSemanticErrorCodeEnum.VP003);

			// Check if whitespace in incoming receiverid and give a hint in the error message if found.
			String whitespaceDetectedHintString = "";
			if (input.receiverId.contains(" ")) {
				whitespaceDetectedHintString = ". Whitespace detected in incoming request!";
			}
			// No Logical Address found for serviceNamespace
			String errorMessage = input.getSummary() + ", From:" + vpInstanceId + whitespaceDetectedHintString;
			raiseError(VpSemanticErrorCodeEnum.VP004, errorMessage);
		}


		final Set<String> rivProfiles = routingInfos.stream().map(RoutingInfo::getRivProfile).collect(Collectors.toSet());
		raiseError(rivProfiles.size() == 0, VpSemanticErrorCodeEnum.VP005, input);
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

		this.validateRequest(input);
		final VisaVagvalRequest request = this.createVisaVagvalRequest(input);


		final List<RoutingInfo> routingInfos = agent.visaVagval(request);
		if (routingInfos.isEmpty()) {

			raiseError(input.receiverId == null, VpSemanticErrorCodeEnum.VP003);

			String whitespaceDetectedHintString = "";
			if (input.receiverId.contains(" ")) {
				whitespaceDetectedHintString = ". Whitespace detected in incoming request!";
			}
			String errorMessage = input.getSummary() + ", From:" + vpInstanceId + whitespaceDetectedHintString;
			raiseError(VpSemanticErrorCodeEnum.VP004, errorMessage);
		}


		return this.getAddressToVirtualService(routingInfos, input);
	}

	private VagvalInput createRequestToServiceDirectory(MuleMessage muleMessage) {

		VagvalInput vagvalInput = new VagvalInput();
		vagvalInput.senderId = muleMessage.getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);
		vagvalInput.receiverId = muleMessage.getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION);
		vagvalInput.rivVersion = muleMessage.getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);
		vagvalInput.serviceContractNamespace = muleMessage.getProperty(VPUtil.SERVICECONTRACT_NAMESPACE, PropertyScope.SESSION);

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

	private String getAddressToVirtualService(final List<RoutingInfo> services, final VagvalInput request) {
		String adress = null;
		int noOfMatchingAdresses = 0;
		for (RoutingInfo vvInfo : services) {
			if (vvInfo.getRivProfile().equals(request.rivVersion)) {
				adress = vvInfo.getAddress();
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
