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

import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

/**
 * Helper class for working with addressing
 *
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class AddressingHelper extends VPHelperSupport {

	private VisaVagvalsInterface agent;

	public AddressingHelper(MuleMessage muleMessage, final VisaVagvalsInterface agent) {
		super(muleMessage);

		this.agent = agent;
	}

	public String getAvailableRivProfile() {

		final VagvalInput input = this.createRequestToServiceDirectory();
		final VisaVagvalResponse response = agent.visaVagval(this.createVisaVagvalRequest(input));

		final List<VirtualiseringsInfoType> virts = this.getAllVirtualizedServices(response, input);

		final Set<String> rivProfiles = new HashSet<String>();
		for (final VirtualiseringsInfoType virt : virts) {
			rivProfiles.add(virt.getRivProfil());
		}

		if (rivProfiles.size() == 0) {
			String errorMessage = ("VP005 No ruting with matching Riv-version found for serviceContractNamespace :"
					+ input.serviceContractNamespace + ", receiverId:" + input.receiverId + "RivVersion" + input.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (rivProfiles.size() > 1) {
			String errorMessage = "VP006 More than one route with matching Riv-version found for serviceContractNamespace:"
					+ input.serviceContractNamespace + ", receiverId:" + input.receiverId;
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return rivProfiles.iterator().next();
	}

	public String getAddress() {
		/*
		 * Create parameters from message
		 */
		final VagvalInput input = this.createRequestToServiceDirectory();
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

	private VagvalInput createRequestToServiceDirectory() {

		VagvalInput vagvalInput = new VagvalInput();
		vagvalInput.senderId = (String) this.getMuleMessage().getProperty(VPUtil.SENDER_ID, PropertyScope.SESSION);
		vagvalInput.receiverId = (String) this.getMuleMessage().getProperty(VPUtil.RECEIVER_ID, PropertyScope.SESSION);
		vagvalInput.rivVersion = (String) this.getMuleMessage().getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);
		vagvalInput.serviceContractNamespace = (String) this.getMuleMessage().getProperty(VPUtil.SERVICECONTRACT_NAMESPACE, PropertyScope.SESSION);

		return vagvalInput;
	}

	private void validateRequest(final VagvalInput request) {
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug(
					"Calling vagvalAgent with serviceNamespace:" + request.serviceContractNamespace + ", receiverId:"
							+ request.receiverId + ", senderId: " + request.senderId);
		}
		if (request.rivVersion == null) {
			String errorMessage = ("VP001 No RIV version configured");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (request.senderId == null) {
			String errorMessage = ("VP002 No sender ID (from_address) found in certificate");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (request.receiverId == null) {
			String errorMessage = ("VP003 No receiver ID (to_address) found in message");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
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
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (noOfMatchingAdresses > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId;
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = ("VP010 Physical Adress field is empty in Service Producer for serviceNamespace :"
					+ request.serviceContractNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return adress;
	}

	private List<VirtualiseringsInfoType> getAllVirtualizedServices(final VisaVagvalResponse response,
			final VagvalInput request) {
		List<VirtualiseringsInfoType> virtualiseringar = response.getVirtualiseringsInfo();
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug("VagvalAgent response count: " + virtualiseringar.size());
			for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
				this.getLog().debug(
						"VagvalAgent response item RivProfil: " + vvInfo.getRivProfil() + ", Address: "
								+ vvInfo.getAdress());
			}
		}

		if (virtualiseringar.size() == 0) {
			String errorMessage = "VP004 No Logical Adress found for serviceNamespace:" + request.serviceContractNamespace
					+ ", receiverId:" + request.receiverId;
			this.getLog().info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return virtualiseringar;
	}
}
