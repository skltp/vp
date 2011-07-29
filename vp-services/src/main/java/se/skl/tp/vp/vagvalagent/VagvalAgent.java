/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.datatype.DatatypeConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheRequest;
import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaAnropsBehorigheterResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaVirtualiseringarResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsInfoInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsServiceSoap11LitDocService;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.ClientUtil;

public class VagvalAgent implements VisaVagvalsInterface {

	private static final Logger logger = LoggerFactory.getLogger(VagvalAgent.class);

	public static final String HSA_SENDERID_VAGVALSAGENT = "vagvalsagent";

	public List<VirtualiseringsInfoType> virtualiseringsInfo = null;
	public List<AnropsBehorighetsInfoType> anropsBehorighetsInfo = null;

	public List<VirtualiseringsInfoType> tempVirtualiseringsInfo = null;
	public List<AnropsBehorighetsInfoType> tempAnropsBehorighetsInfo = null;

	private String endpointAddressTjanstekatalog;
	private String addressDelimiter;

	public void setEndpointAddress(String endpointAddressTjanstekatalog) {
		this.endpointAddressTjanstekatalog = endpointAddressTjanstekatalog;
	}

	public void setAddressDelimiter(String addressDelimiter) {
		this.addressDelimiter = addressDelimiter;
	}

	/**
	 * Initialize the two lists if they are null
	 */
	public void init() {
		if (anropsBehorighetsInfo == null || virtualiseringsInfo == null) {
			synchronized (this) {

				if (logger.isDebugEnabled()) {
					logger.debug("entering VagvalsAgent.init");
				}
				
				try {
					
					logger.info("Fetch all permissions...");
					
					HamtaAllaAnropsBehorigheterResponseType respAllaAnropsBehorigheter = getPort()
							.hamtaAllaAnropsBehorigheter(null);

					logger.info("Fetch all virtualizations...");
					
					HamtaAllaVirtualiseringarResponseType respAllaVirtualiseringar = getPort()
							.hamtaAllaVirtualiseringar(null);

					logger.info("Fetch request permission information.");
					
					anropsBehorighetsInfo = respAllaAnropsBehorigheter.getAnropsBehorighetsInfo();
					
					logger.info("Fetch virtualization information");
					
					virtualiseringsInfo = respAllaVirtualiseringar.getVirtualiseringsInfo();
					if (logger.isDebugEnabled()) {
						logger.info("init loaded " + anropsBehorighetsInfo.size()
								+ " AnropsBehorighet");
						logger.info("init loaded " + virtualiseringsInfo.size()
								+ " VirtualiseradTjansteproducent");
					}
				} catch (RuntimeException e) {
					
					e.printStackTrace();
					
					logger.error("Exception in VagvalsAgewn.init() caused by" + e.toString());
				}
			}
		}
	}

	private SokVagvalsInfoInterface getPort() {
		SokVagvalsServiceSoap11LitDocService service = new SokVagvalsServiceSoap11LitDocService(
				ClientUtil.createEndpointUrlFromServiceAddress(endpointAddressTjanstekatalog));
		SokVagvalsInfoInterface port = service.getSokVagvalsSoap11LitDocPort();
		return port;
	}

	/**
	 * Resets the cached info
	 */
	public void reset() {
		anropsBehorighetsInfo = null;
		virtualiseringsInfo = null;
	}

	/**
	 * 
	 */
	public ResetVagvalCacheResponse resetVagvalCache(
			ResetVagvalCacheRequest parameters) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent resetVagvalCache");
		}

		Boolean result = false;
		
		synchronized(this) {
			// Save old config information
			tempVirtualiseringsInfo = virtualiseringsInfo;
			tempAnropsBehorighetsInfo = anropsBehorighetsInfo;
	
			reset();
			init();
	
			// Check outcome of init, ie should we use new configuration!
			if (virtualiseringsInfo == null || anropsBehorighetsInfo == null) {
				virtualiseringsInfo = tempVirtualiseringsInfo;
				anropsBehorighetsInfo = tempAnropsBehorighetsInfo;
				result = false;
			} else {
				tempVirtualiseringsInfo = null;
				tempAnropsBehorighetsInfo = null;	
				result = true;
			}
		}
		
		ResetVagvalCacheResponse response = new ResetVagvalCacheResponse();
		response.setResetResult(result);

		return response;
	}
	
	/**
	 * 
	 * @param parameters
	 *            Receiver, Sender, ServiceName(TjansteKontrakt namespace), Time
	 * @throws VpSemanticException
	 *             if no AnropsBehorighet is found
	 */
	public VisaVagvalResponse visaVagval(VisaVagvalRequest parameters) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent visaVagval");
		}
		// If the initiation failed, try again
		init();

		if (anropsBehorighetsInfo == null || virtualiseringsInfo == null) {
			String errorMessage = "VP008 No contact with Tjanstekatalogen at startup or now, not possible to route call";
			logger.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		VisaVagvalResponse response = new VisaVagvalResponse();
		
		// Determine if delimiter is set and present in request logical address
		boolean isDelimiterPresent = addressDelimiter != null && addressDelimiter.length() > 0 && parameters.getReceiverId().contains(addressDelimiter);
		
		// Extract all separate addresses in receiverId if it contains delimiter character
		List<String> receiverAddresses = new ArrayList<String>();
		if (isDelimiterPresent) {
			StringTokenizer strToken = new StringTokenizer(parameters.getReceiverId(), addressDelimiter);
			while(strToken.hasMoreTokens() ) {
				String tempAddress = (String) strToken.nextElement();
				if (!receiverAddresses.contains(tempAddress)) {
					receiverAddresses.add(0, tempAddress);
				}
			}		
		} else {
			receiverAddresses.add(parameters.getReceiverId());
		}
		
		// Outer loop over all given addresses
		boolean addressFound = false;
		for (String requestReceiverId : receiverAddresses) {
			// Find all possible LogiskAdressat
			for (VirtualiseringsInfoType vi : virtualiseringsInfo) {
				if (vi.getReceiverId().equals(requestReceiverId)
						&& vi.getTjansteKontrakt().equals(parameters.getTjanstegranssnitt())
						&& parameters.getTidpunkt().compare(vi.getFromTidpunkt()) != DatatypeConstants.LESSER
						&& parameters.getTidpunkt().compare(vi.getTomTidpunkt()) != DatatypeConstants.GREATER) {
					addressFound = true;
					response.getVirtualiseringsInfo().add(vi);
				}
			}
			// Only return 1 address if we do a delimiter search!
			if (isDelimiterPresent && addressFound) {
				break;
			}
		}
		
		if (response.getVirtualiseringsInfo().size() == 0) {
			return response;
		}

		// Check authorization
		Boolean behorighetsStatus = false;
		// Outer loop over all given addresses
		for (String requestReceiverId : receiverAddresses) {
			for (AnropsBehorighetsInfoType abi : anropsBehorighetsInfo) {	
				if (abi.getReceiverId().equals(requestReceiverId)
						&& abi.getSenderId().equals(parameters.getSenderId())
						&& abi.getTjansteKontrakt().equals(parameters.getTjanstegranssnitt())
						&& parameters.getTidpunkt().compare(abi.getFromTidpunkt()) != DatatypeConstants.LESSER
						&& parameters.getTidpunkt().compare(abi.getTomTidpunkt()) != DatatypeConstants.GREATER) {
					behorighetsStatus = true;
				}
			}
		}
		
		if (behorighetsStatus == false) {
			String errorMessage = ("VP007 Authorization missing for serviceNamespace: "
					+ parameters.getTjanstegranssnitt() + ", receiverId: "
					+ parameters.getReceiverId() + ", senderId: " + parameters
					.getSenderId());
			// This is not a fatal error, but if it happens a sign that a
			// calling application does not know what its doing
			// Or that there is a misconfiguration in the TjansteKatalog
			// component
			// Normally you should only call services that you have authority to
			// use, thats why we use info level
			logger.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return response;
	}
}
