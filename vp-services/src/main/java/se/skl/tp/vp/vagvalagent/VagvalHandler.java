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

import static se.skl.tp.hsa.cache.HsaCache.DEFAUL_ROOTNODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConstants;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheInitializationException;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.MdcLogTrace;

public class VagvalHandler {

	// Initialized to a non-null value by the constructor.
	private List<VirtualiseringsInfoType> virtualiseringsInfo = null;
	private Map<String, List<VirtualiseringsInfoType>> virtualiseringsInfoMap;

	// Initialized to a non-null value by the constructor.
	private HsaCache hsaCache;

	public VagvalHandler(HsaCache hsaCache, List<VirtualiseringsInfoType> virtualiseringsInfo) {

		if (hsaCache == null) {
			throw new RuntimeException("Null is not allowed for the parameter hsaCache");
		}
		if (virtualiseringsInfo == null) {
			throw new RuntimeException("Null is not allowed for the parameter virtualiseringsInfo");
		}

		this.hsaCache = hsaCache;
		this.virtualiseringsInfo = virtualiseringsInfo;
		this.virtualiseringsInfoMap = createVirtualiseringsInfoMap(virtualiseringsInfo);
	}

	public int size() {
		return virtualiseringsInfo.size();
	}

	public List<VirtualiseringsInfoType> getVirtualiseringsInfo() {
		return virtualiseringsInfo;
	}

	public VisaVagvalResponse getRoutingInformation(VisaVagvalRequest request, boolean useDeprecatedDefaultRouting, List<String> receiverAddresses) {
		// Check if routing was found in TAK for requested receiver.
		VisaVagvalResponse response = getRoutingInformationFromLeaf(request, useDeprecatedDefaultRouting, receiverAddresses);

		// logTrace
		if (!containsNoRouting(response)) {
			StringBuilder logTrace = new StringBuilder();
			logTrace.append("(leaf)");
			for (String ra : receiverAddresses) {
				logTrace.append(ra);
				logTrace.append(",");
				
			}
			logTrace.deleteCharAt(logTrace.length() -1);
			MdcLogTrace.put(MdcLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE, logTrace.toString());
		}

		// No routing information found for requested receiver, check in the HSA
		// tree if there is any routing information registered on parents.
		// Routing using HSA tree is only applicable when not using deprecated
		// default routing (VG#VE)
		if (containsNoRouting(response) && !useDeprecatedDefaultRouting) {
			response = getRoutingInformationFromParent(request, receiverAddresses);
		}
		return response;
	}

	public boolean containsNoRouting(VisaVagvalResponse response) {
		return response == null || response.getVirtualiseringsInfo().isEmpty();
	}

	public VisaVagvalResponse getRoutingInformationFromLeaf(VisaVagvalRequest request, boolean useDeprecatedDefaultRouting, List<String> receiverAddresses) {

		VisaVagvalResponse response = new VisaVagvalResponse();

		// Outer loop over all given addresses
		boolean addressFound = false;
		for (String requestReceiverId : receiverAddresses) {
			// Find all possible LogiskAdressat

			// Start to lookup elements matching recevier, tjanstekontrakt in the map
			List<VirtualiseringsInfoType> matchingVirtualiseringsInfo = lookupInVirtualiseringsInfoMap(requestReceiverId, request.getTjanstegranssnitt());

			// Skip if no hit in the map
			if (matchingVirtualiseringsInfo != null) {

				// Now look through that list for matching time intervals
				for (VirtualiseringsInfoType vi : matchingVirtualiseringsInfo) {
					
					// Create year+month+day ints for easy comparing dates, shift year all to the left, then month and last day e.g. 20150123
					int requestDate = request.getTidpunkt().getYear()*10000 + request.getTidpunkt().getMonth()*100 + request.getTidpunkt().getDay();
					int virtFromDate = vi.getFromTidpunkt().getYear()*10000 + vi.getFromTidpunkt().getMonth()*100 + vi.getFromTidpunkt().getDay();
					int virtTomDate = vi.getTomTidpunkt().getYear()*10000 + vi.getTomTidpunkt().getMonth()*100 + vi.getTomTidpunkt().getDay();
									
					if (requestDate >= virtFromDate && requestDate <= virtTomDate) {
						addressFound = true;
						response.getVirtualiseringsInfo().add(vi);
					}
				}
			}

			// Only return 1 address if we do a delimiter search!
			if (useDeprecatedDefaultRouting && addressFound) {
				break;
			}
		}
		return response;
	}

	public VisaVagvalResponse getRoutingInformationFromParent(VisaVagvalRequest request, List<String> receiverAddresses) {

		VisaVagvalResponse response = new VisaVagvalResponse();
		String receiverId = receiverAddresses.get(0);
		
		StringBuilder logTrace = new StringBuilder();
		logTrace.append("(parent)");
		logTrace.append(receiverId);
		logTrace.append(",");

		while (response.getVirtualiseringsInfo().isEmpty() && receiverId != DEFAUL_ROOTNODE) {
			receiverId = getParent(receiverId);
			
			logTrace.append(receiverId);
			logTrace.append(",");
			
			// FIXME: When deprecated default routing is removed
			// construction using Arrays.asList(receiverId) can be replaced
			// with String
			response = getRoutingInformationFromLeaf(request, false, Arrays.asList(receiverId));
		}
		
		logTrace.deleteCharAt(logTrace.length() -1);
		MdcLogTrace.put(MdcLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE, logTrace.toString());

		return response;
	}

	private String getParent(String receiverId) {
		try {
			return hsaCache.getParent(receiverId);
		} catch (HsaCacheInitializationException e) {
			throw new VpSemanticException(VpSemanticErrorCodeEnum.VP011 + " Internal HSA cache is not available!", VpSemanticErrorCodeEnum.VP011, e);
		}
	}

	/* VIRTUALISERINGS-INFO MAP METHODS */

	private Map<String, List<VirtualiseringsInfoType>> createVirtualiseringsInfoMap(List<VirtualiseringsInfoType> inVirtualiseringsInfo) {

		Map<String, List<VirtualiseringsInfoType>> outVirtualiseringsInfoMap = new HashMap<String, List<VirtualiseringsInfoType>>();

		// Loop through all entries in the list and store them by recevier and tjanstekontrakt in a map for faster lookup
		for (VirtualiseringsInfoType v : inVirtualiseringsInfo) {
			String key = createVirtualiseringsInfoMapKey(v.getReceiverId(), v.getTjansteKontrakt());

			// Lookup the entry (list) in the map and create it if missing
			List<VirtualiseringsInfoType> value = outVirtualiseringsInfoMap.get(key);
			if (value == null) {
				value = new ArrayList<VirtualiseringsInfoType>();
				outVirtualiseringsInfoMap.put(key, value);
			}

			// Add the record to the list
			value.add(v);
		}

		return outVirtualiseringsInfoMap;
	}

	List<VirtualiseringsInfoType> lookupInVirtualiseringsInfoMap(String receiverId, String tjansteKontrakt) {
		String key = createVirtualiseringsInfoMapKey(receiverId, tjansteKontrakt);
		return virtualiseringsInfoMap.get(key);
	}

	private String createVirtualiseringsInfoMapKey(String receiverId, String tjansteKontrakt) {
		return receiverId + "|" + tjansteKontrakt;
	}
}
