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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;

import se.skl.tp.hsa.cache.HsaCache;
import static se.skl.tp.hsa.cache.HsaCache.DEFAUL_ROOTNODE;
import se.skl.tp.hsa.cache.HsaCacheInitializationException;
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

	// cache
	@XmlRootElement
	static class PersistentCache implements Serializable {
		private static final long serialVersionUID = 1L;
		@XmlElement
		private List<VirtualiseringsInfoType> virtualiseringsInfo;
		@XmlElement
		private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;
	}

	private static final JaxbUtil JAXB = new JaxbUtil(PersistentCache.class);

	// file name of local cache
	public static final String TK_LOCAL_CACHE = System.getProperty("user.home") + System.getProperty("file.separator")
			+ ".tk.localCache";

	public List<VirtualiseringsInfoType> virtualiseringsInfo = null;
	public List<AnropsBehorighetsInfoType> anropsBehorighetsInfo = null;
	private HsaCache hsaCache;

	private String endpointAddressTjanstekatalog;
	private String addressDelimiter;

	public VagvalAgent() {

	}

	public void setEndpointAddress(String endpointAddressTjanstekatalog) {
		this.endpointAddressTjanstekatalog = endpointAddressTjanstekatalog;
	}

	public void setAddressDelimiter(String addressDelimiter) {
		this.addressDelimiter = addressDelimiter;
	}

	public void setHsaCache(HsaCache hsaCache) {
		this.hsaCache = hsaCache;
	}

	/**
	 * Initialize the two lists if they are null
	 */
	public void init() {
		if (!isInitialized()) {
			if (logger.isDebugEnabled()) {
				logger.debug("entering VagvalsAgent.init");
			}

			setState(getVirtualiseringar(), getBehorigheter());

			if (isInitialized()) {
				saveToLocalCopy(TK_LOCAL_CACHE);
			} else {
				restoreFromLocalCopy(TK_LOCAL_CACHE);
			}

			if (isInitialized() && logger.isDebugEnabled()) {
				logger.info("init loaded " + anropsBehorighetsInfo.size() + " AnropsBehorighet");
				logger.info("init loaded " + virtualiseringsInfo.size() + " VirtualiseradTjansteproducent");
			}
		}
	}

	/**
	 * Sets state.
	 * 
	 * @param v
	 *            the virtualization state.
	 * @param p
	 *            the permission state.
	 */
	private synchronized void setState(List<VirtualiseringsInfoType> v, List<AnropsBehorighetsInfoType> p) {
		this.virtualiseringsInfo = v;
		this.anropsBehorighetsInfo = p;
	}

	/**
	 * Return if cache has been initialized.
	 * 
	 * @return true if cache has been initalized, otherwise false.
	 */
	private synchronized boolean isInitialized() {
		return (this.anropsBehorighetsInfo != null) && (this.virtualiseringsInfo != null);
	}

	private SokVagvalsInfoInterface getPort() {
		SokVagvalsServiceSoap11LitDocService service = new SokVagvalsServiceSoap11LitDocService(
				ClientUtil.createEndpointUrlFromServiceAddress(endpointAddressTjanstekatalog));
		SokVagvalsInfoInterface port = service.getSokVagvalsSoap11LitDocPort();
		return port;
	}

	/**
	 * Return virtualizations from TK, or from local cache if TK is unavailable
	 * 
	 * @return virtualizations, or null on any error.
	 */
	private List<VirtualiseringsInfoType> getVirtualiseringar() {
		List<VirtualiseringsInfoType> l = null;
		try {
			logger.info("Fetch all virtualizations...");
			HamtaAllaVirtualiseringarResponseType t = getPort().hamtaAllaVirtualiseringar(null);
			l = t.getVirtualiseringsInfo();
		} catch (Exception e) {
			logger.error("Unable to get virtualizations", e);
		}
		return l;
	}

	/**
	 * Return permissions from TK, or from local cache if TK is unavailable
	 * 
	 * @return permissions, or null on any error.
	 */
	private List<AnropsBehorighetsInfoType> getBehorigheter() {
		List<AnropsBehorighetsInfoType> l = null;
		try {
			logger.info("Fetch all permissions...");
			HamtaAllaAnropsBehorigheterResponseType t = getPort().hamtaAllaAnropsBehorigheter(null);
			l = t.getAnropsBehorighetsInfo();
		} catch (Exception e) {
			logger.error("Unable to get permissions", e);
		}
		return l;
	}

	// restore saved object
	private void restoreFromLocalCopy(String fileName) {
		PersistentCache pc = null;
		InputStream is = null;
		final File file = new File(fileName);
		try {
			if (file.exists()) {
				logger.info("Restore from local copy: {}", fileName);
				is = new FileInputStream(file);
				pc = (PersistentCache) JAXB.unmarshal(is);
			}
		} catch (Exception e) {
			logger.error("Unable to restore from: " + fileName, e);
			// remove erroneous file.
			if (is != null) {
				file.delete();
			}
		} finally {
			close(is);
		}

		setState((pc == null) ? null : pc.virtualiseringsInfo, (pc == null) ? null : pc.anropsBehorighetsInfo);
	}

	// save object
	private void saveToLocalCopy(String fileName) {
		PersistentCache pc = new PersistentCache();
		pc.anropsBehorighetsInfo = this.anropsBehorighetsInfo;
		pc.virtualiseringsInfo = this.virtualiseringsInfo;

		logger.info("Save to local copy: {}", fileName);
		OutputStream os = null;
		try {
			File file = new File(fileName);
			os = new FileOutputStream(file);
			os.write(JAXB.marshal(pc).getBytes("UTF-8"));
		} catch (Exception e) {
			logger.error("Unable to save state to: " + fileName, e);
		} finally {
			close(os);
		}
	}

	// close resource, ignore errors
	private static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Resets the cached info
	 */
	public void reset() {
		setState(null, null);
	}

	/**
	 * Resets cache.
	 */
	public ResetVagvalCacheResponse resetVagvalCache(ResetVagvalCacheRequest parameters) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent resetVagvalCache");
		}

		reset();
		init();

		ResetVagvalCacheResponse response = new ResetVagvalCacheResponse();

		if (!isInitialized()) {
			response.setResetResult(false);
		} else {
			response.setResetResult(true);
		}

		return response;
	}

	/**
	 * 
	 * @param request
	 *            Receiver, Sender, ServiceName(TjansteKontrakt namespace), Time
	 * @throws VpSemanticException
	 *             if no AnropsBehorighet is found
	 */
	public VisaVagvalResponse visaVagval(VisaVagvalRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent visaVagval");
		}
		// If the initiation failed, try again
		init();

		if (!isInitialized()) {
			String errorMessage = "VP008 No contact with Tjanstekatalogen at startup, and no local cache to fallback on, not possible to route call";
			logger.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		final List<VirtualiseringsInfoType> curVirtualiseringsInfo;
		final List<AnropsBehorighetsInfoType> curAnropsBehorighetsInfo;
		synchronized (this) {
			curVirtualiseringsInfo = this.virtualiseringsInfo;
			curAnropsBehorighetsInfo = this.anropsBehorighetsInfo;
		}

		// Determine if delimiter is set and present in request logical address.
		// Delimiter is used in deprecated default routing (VG#VE).
		boolean useDeprecatedDefaultRouting = addressDelimiter != null && addressDelimiter.length() > 0
				&& request.getReceiverId().contains(addressDelimiter);
		List<String> receiverAddresses = extractReceiverAdresses(request, useDeprecatedDefaultRouting);

		// Check if routing was found in TAK for requested receiver.
		VisaVagvalResponse response = getRoutingInformation(request, curVirtualiseringsInfo,
				useDeprecatedDefaultRouting, receiverAddresses);

		// No routing information found for requested receiver, check in the HSA
		// tree if there is any routing information registered on parents.
		// Routing using HSA tree is only applicable when not using deprecated
		// default routing (VG#VE)
		if (containsNoRouting(response) && !useDeprecatedDefaultRouting) {
			response = getRoutingInformationFromParent(request, curVirtualiseringsInfo, receiverAddresses);
		}

		// No routing was found neither on requested receiver nor using the HSA
		// tree for parents. No need to continue to check authorization.
		if (containsNoRouting(response)) {
			return response;
		}

		// Check in TAK if sender is authorized to call the requested
		// receiver,if not check if sender is authorized to call any of the
		// receiver parents using HSA tree. HSA tree is only used when old
		// school default routing (VG#VE) is not used.
		if (!isAuthorized(request, receiverAddresses, curAnropsBehorighetsInfo)) {
			if (!isAuthorizedToAnyParent(request, curAnropsBehorighetsInfo)) {
				throwNotAuthorizedException(request);
			}
		}

		return response;
	}

	private VisaVagvalResponse getRoutingInformation(VisaVagvalRequest request,
			final List<VirtualiseringsInfoType> curVirtualiseringsInfo, boolean useDeprecatedDefaultRouting,
			List<String> receiverAddresses) {
		VisaVagvalResponse response = new VisaVagvalResponse();

		// Outer loop over all given addresses
		boolean addressFound = false;
		for (String requestReceiverId : receiverAddresses) {
			// Find all possible LogiskAdressat
			for (VirtualiseringsInfoType vi : curVirtualiseringsInfo) {
				if (vi.getReceiverId().equals(requestReceiverId)
						&& vi.getTjansteKontrakt().equals(request.getTjanstegranssnitt())
						&& request.getTidpunkt().compare(vi.getFromTidpunkt()) != DatatypeConstants.LESSER
						&& request.getTidpunkt().compare(vi.getTomTidpunkt()) != DatatypeConstants.GREATER) {
					addressFound = true;
					response.getVirtualiseringsInfo().add(vi);
				}
			}
			// Only return 1 address if we do a delimiter search!
			if (useDeprecatedDefaultRouting && addressFound) {
				break;
			}
		}
		return response;
	}

	private VisaVagvalResponse getRoutingInformationFromParent(VisaVagvalRequest request,
			final List<VirtualiseringsInfoType> curVirtualiseringsInfo, List<String> receiverAddresses) {

		VisaVagvalResponse response = new VisaVagvalResponse();

		String receiverId = receiverAddresses.get(0);
		while (response.getVirtualiseringsInfo().isEmpty() && receiverId != DEFAUL_ROOTNODE) {
			receiverId = getParent(receiverId);
			// FIXME: When deprecated default routing is removed
			// construction using Arrays.asList(receiverId) can be replaced
			// with String
			response = getRoutingInformation(request, curVirtualiseringsInfo, false, Arrays.asList(receiverId));
		}

		return response;
	}

	private String getParent(String receiverId) {
		try {
			return hsaCache.getParent(receiverId);
		} catch (HsaCacheInitializationException e) {
			throw new VpSemanticException("VP011 Internal HSA cache is not available!", e);
		}
	}

	private boolean containsNoRouting(VisaVagvalResponse response) {
		return response == null || response.getVirtualiseringsInfo().isEmpty();
	}

	/*
	 * Extract all separate addresses in receiverId if it contains delimiter
	 * character.
	 */
	private List<String> extractReceiverAdresses(VisaVagvalRequest request, boolean useDeprecatedDefaultRouting) {
		List<String> receiverAddresses = new ArrayList<String>();
		if (useDeprecatedDefaultRouting) {
			StringTokenizer strToken = new StringTokenizer(request.getReceiverId(), addressDelimiter);
			while (strToken.hasMoreTokens()) {
				String tempAddress = (String) strToken.nextElement();
				if (!receiverAddresses.contains(tempAddress)) {
					receiverAddresses.add(0, tempAddress);
				}
			}
		} else {
			receiverAddresses.add(request.getReceiverId());
		}
		return receiverAddresses;
	}

	private void throwNotAuthorizedException(VisaVagvalRequest request) {
		String errorMessage = ("VP007 Authorization missing for serviceNamespace: " + request.getTjanstegranssnitt()
				+ ", receiverId: " + request.getReceiverId() + ", senderId: " + request.getSenderId());
		logger.info(errorMessage);
		throw new VpSemanticException(errorMessage);
	}

	private boolean isAuthorized(VisaVagvalRequest request, List<String> receiverAddresses,
			List<AnropsBehorighetsInfoType> permissions) {
		for (String requestReceiverId : receiverAddresses) {
			if (isAuthorized(request, requestReceiverId, permissions)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAuthorizedToAnyParent(VisaVagvalRequest request, List<AnropsBehorighetsInfoType> permissions) {
		String receiverId = request.getReceiverId();
		while (receiverId != DEFAUL_ROOTNODE) {
			receiverId = getParent(receiverId);
			if (isAuthorized(request, receiverId, permissions)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAuthorized(VisaVagvalRequest request, String receiverId,
			List<AnropsBehorighetsInfoType> permissions) {
		for (AnropsBehorighetsInfoType abi : permissions) {
			if (abi.getReceiverId().equals(receiverId) && abi.getSenderId().equals(request.getSenderId())
					&& abi.getTjansteKontrakt().equals(request.getTjanstegranssnitt())
					&& request.getTidpunkt().compare(abi.getFromTidpunkt()) != DatatypeConstants.LESSER
					&& request.getTidpunkt().compare(abi.getTomTidpunkt()) != DatatypeConstants.GREATER) {
				return true;
			}
		}
		return false;
	}
}
