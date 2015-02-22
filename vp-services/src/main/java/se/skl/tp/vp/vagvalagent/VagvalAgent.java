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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;

import se.skl.tp.hsa.cache.HsaCache;
import se.skltp.tak.vagval.wsdl.v2.ResetVagvalCacheRequest;
import se.skltp.tak.vagval.wsdl.v2.ResetVagvalCacheResponse;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalsInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaAnropsBehorigheterResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaVirtualiseringarResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.SokVagvalsInfoInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.SokVagvalsServiceSoap11LitDocService;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.ClientUtil;

/**
 * Provides routing information.
 * <p>Implementation notes: access to internal state in this class must be
 * thread-safe since:</p>
 * <ol>
 * <li>TAK-data is loaded during startup (using init), only one thread is
 * allowed to load TAK-data</li>
 * <li>TAK-data can be refreshed from TAK during operation</li>
 * </ol> 
 */
public class VagvalAgent implements VisaVagvalsInterface {

	private static final Logger logger = LoggerFactory.getLogger(VagvalAgent.class);
	public static final boolean FORCE_RESET = true;
	public static final boolean DONT_FORCE_RESET = false;

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

	private String localTakCache;

	private VagvalHandler vagvalHandler = null;
	private BehorighetHandler behorighetHandler = null;
	private HsaCache hsaCache;

	private String endpointAddressTjanstekatalog;
	private String addressDelimiter;

	private SokVagvalsInfoInterface port = null;
	
	private Object lockTakFetch = new Object();

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

	public void setLocalTakCache(String localTakCache) {
		this.localTakCache = localTakCache;
	}

	/**
	 * Initialize VagvalAgent resources. Force a init by setting forceReset=true, use
	 * constants VagvalAgent.FORCE_RESET or VagvalAgent.DONT_FORCE_RESET.
	 * If not forced, init checks if necessary resources are loaded, otherwise
	 * resources are loaded.
	 *
	 * @param forceReset Force a init by setting forceReset=true
	 * @return a processing log containing status for loading TAK resources
	 */
	public VagvalAgentProcessingLog init(boolean forceReset) {
		VagvalAgentProcessingLog processingLog = new VagvalAgentProcessingLog();
		if (!forceReset) {
			init(processingLog);
		}
		else {
			boolean isRefreshSuccessful = refresh(processingLog);
			processingLog.isRefreshRequested = forceReset;
			processingLog.isRefreshSuccessful = isRefreshSuccessful;			
		}
		return processingLog;
	}
	
	/**
	 * Thread-safe initialization, allow only one thread to do initialization.
	 * 
	 * @param processingLog
	 */
	private synchronized void init(VagvalAgentProcessingLog processingLog) {
		if (!isInitialized()) {
			String logMsg = "init: not initialized, will do init ...";
			logger.info(logMsg);
			processingLog.addLog(logMsg);
			refresh(processingLog);
			logMsg = "init done, was successful: " + isInitialized();
			logger.info(logMsg);
			processingLog.addLog(logMsg);
		}		
	}
	
	/**
	 * Fetch data from TAK, only update local state if fetch of TAK data is successful. 
	 * <p>Update of local state must be thread-safe, but <b>fetching data from TAK must
	 * not lock/force synchronize access to the local state since that will take some
	 * time</b>, and we can't block reading local state during that time.  
	 * 
	 * @return true if refresh was successful
	 */	
	private boolean refresh(VagvalAgentProcessingLog processingLog) {
		
		boolean isRefreshSuccessful = false;
		
		// only let one thread at a time attempt to fetch and persist TAK data
		synchronized (lockTakFetch) {

			logger.info("Initialize VagvalAgent TAK resources...");
			processingLog.addLog("Initialize VagvalAgent TAK resources...");

			try {
				// both TAK calls to fetch data must succeed to have a consistent state
				List<VirtualiseringsInfoType> v = getVirtualiseringar();
				List<AnropsBehorighetsInfoType> p = getBehorigheter();
				if (v != null && p != null) {
					// do thread-safe update of state
					setState(v, p);
					isRefreshSuccessful = true;
				}
			}
			catch (Exception e) {				
				logger.error("Failed to refresh TAK data", e);
			}

			if (isInitialized()) {
			    processingLog.addLog("Succeeded to get virtualizations and/or permissions from TAK, save to local TAK copy...");
				saveToLocalCopy(localTakCache, processingLog);
			} else {
			    processingLog.addLog("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy...");
				restoreFromLocalCopy(localTakCache, processingLog);
			}

			if (isInitialized()) {
				logger.info("Init VagvalAgent loaded number of permissions: {}", behorighetHandler.size());
				logger.info("Init VagvalAgent loaded number of virtualizations: {}", vagvalHandler.size());
				processingLog.addLog("Init VagvalAgent loaded number of permissions: " + behorighetHandler.size());
				processingLog.addLog("Init VagvalAgent loaded number of virtualizations: " + vagvalHandler.size());
			}

			logger.info("Init VagvalAgent done");
		}
		return isRefreshSuccessful;
	}

	/**
	 * Sets state, must be thread-safe.
	 *
	 * @param v
	 *            the virtualization state.
	 * @param p
	 *            the permission state.
	 */
	private synchronized void setState(List<VirtualiseringsInfoType> v, List<AnropsBehorighetsInfoType> p) {
		this.vagvalHandler = new VagvalHandler(hsaCache, v);
		this.behorighetHandler = new BehorighetHandler(hsaCache, p);
	}

	/**
	 * Return if cache has been initialized.
	 *
	 * @return true if cache has been initalized, otherwise false.
	 */
	private synchronized boolean isInitialized() {
		return (this.behorighetHandler != null) && (this.vagvalHandler != null);
	}

	private SokVagvalsInfoInterface getPort() {
	    if(port == null){
	    	logger.info("Use TAK endpoint adress: {}", endpointAddressTjanstekatalog);
	        SokVagvalsServiceSoap11LitDocService service = new SokVagvalsServiceSoap11LitDocService(
	                ClientUtil.createEndpointUrlFromServiceAddress(endpointAddressTjanstekatalog));
	        port = service.getSokVagvalsSoap11LitDocPort();
	    }
		return port;
	}

	protected void setPort(SokVagvalsInfoInterface port) {
        this.port = port;
    }

	/**
	 * Return virtualizations from TK, or from local cache if TK is unavailable
	 *
	 * @return virtualizations, or null on any error.
	 */
	protected List<VirtualiseringsInfoType> getVirtualiseringar() throws Exception {
		List<VirtualiseringsInfoType> l = null;
		try {
			logger.info("Fetch all virtualizations from TAK...");
			HamtaAllaVirtualiseringarResponseType t = getPort().hamtaAllaVirtualiseringar(null);
			l = t.getVirtualiseringsInfo();
		} catch (Exception e) {
			logger.error("Unable to get virtualizations from TAK", e);
			throw e;
		}
		return l;
	}

	/**
	 * Return permissions from TK, or from local cache if TK is unavailable
	 *
	 * @return permissions, or null on any error.
	 */
	protected List<AnropsBehorighetsInfoType> getBehorigheter() throws Exception {
		List<AnropsBehorighetsInfoType> l = null;
		try {
			logger.info("Fetch all permissions from TAK...");
			HamtaAllaAnropsBehorigheterResponseType t = getPort().hamtaAllaAnropsBehorigheter(null);
			l = t.getAnropsBehorighetsInfo();
		} catch (Exception e) {
			logger.error("Unable to get permissions from TAK", e);
			throw e;
		}
		return l;
	}

	// restore saved object
	private void restoreFromLocalCopy(String fileName, VagvalAgentProcessingLog processingLog) {
		PersistentCache pc = null;
		InputStream is = null;
		final File file = new File(fileName);
		try {
			if (file.exists()) {
				logger.info("Restore virtualizations and permissions from local TAK copy: {}", fileName);
				is = new FileInputStream(file);
				pc = (PersistentCache) JAXB.unmarshal(is);
				processingLog.addLog("Succesfully restored virtualizations and permissions from local TAK copy: " + fileName);
			}else{
			    logger.error("Failed to find following file containing local TAK copy: {}", fileName);
	            processingLog.addLog("Failed to find following file containing local TAK copy: " + fileName);
			}
		} catch (Exception e) {
			logger.error("Failed to restore virtualizations and permissions from local TAK copy: {}", fileName, e);
			processingLog.addLog("Failed to restore virtualizations and permissions from local TAK copy: " + fileName);
			processingLog.addLog("Reason for failure: " + e.getMessage());

			// remove erroneous file.
			if (is != null) {
				file.delete();
			}
		} finally {
			close(is);
		}

		if (pc != null && pc.anropsBehorighetsInfo != null && pc.virtualiseringsInfo != null) {
			setState(pc.virtualiseringsInfo, pc.anropsBehorighetsInfo);
		}
	}

	// save object
	private void saveToLocalCopy(String fileName, VagvalAgentProcessingLog processingLog) {
		PersistentCache pc = new PersistentCache();
		pc.anropsBehorighetsInfo = this.behorighetHandler.getAnropsBehorighetsInfoList();
		pc.virtualiseringsInfo = this.vagvalHandler.getVirtualiseringsInfo();

		logger.info("Save virtualizations and permissions to local TAK copy: {}", fileName);
		OutputStream os = null;
		try {
			File file = new File(fileName);
			os = new FileOutputStream(file);
			os.write(JAXB.marshal(pc).getBytes("UTF-8"));
			processingLog.addLog("Succesfully saved virtualizations and permissions to local TAK copy: " + fileName);
		} catch (Exception e) {
			logger.error("Failed to save virtualizations and permissions to local TAK copy: {}" + fileName, e);
			processingLog.addLog("Failed to save virtualizations and permissions to local TAK copy: " + fileName);
			processingLog.addLog("Reason for failure: " + e.getMessage());
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
	 * @deprecated since VP 2.10, should be removed when the webservice
	 * GetLogicalAddresseesByServiceContract is no longer produced by VP
	 *  
	 * Get authorization list from the internal TAK cache.
	 * @return list of authorization, empty if no authorizations exists.
	 */
	@Deprecated
	public synchronized List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {
		init(DONT_FORCE_RESET);
		return (behorighetHandler == null) ? Collections.<AnropsBehorighetsInfoType>emptyList() : behorighetHandler.getAnropsBehorighetsInfoList();
	}

	/**
	 * @deprecated since VP 2.10, should be removed when the webservice
	 * GetLogicalAddresseesByServiceContract is no longer produced by VP
	 *   
	 * Get routing information list from the internal TAK cache.
	 * @return list of routing information, empty if no routing information exists.
	 */
	@Deprecated
	public synchronized List<VirtualiseringsInfoType> getVirtualiseringsInfo() {
		init(DONT_FORCE_RESET);
        return (vagvalHandler == null) ? Collections.<VirtualiseringsInfoType>emptyList(): vagvalHandler.getVirtualiseringsInfo();
    }

	/**
	 * Resets cache.
	 */
	public ResetVagvalCacheResponse resetVagvalCache(ResetVagvalCacheRequest parameters) {
		logger.info("Start force a reset of VagvalAgent...");

		ResetVagvalCacheResponse response = new ResetVagvalCacheResponse();

		//Force reset in init
        VagvalAgentProcessingLog processingLog = init(FORCE_RESET);

		if (!processingLog.isRefreshSuccessful) {
			response.setResetResult(false);
			logger.info("Failed force reset VagvalAgent");
		} else {
			response.setResetResult(true);
			logger.info("Successfully force reset VagvalAgent");
		}

		response.getProcessingLog().addAll(processingLog.getLog());
		return response;
	}

	/**
	 *
	 * @param request
	 *            Receiver, Sender, ServiceName(TjansteKontrakt namespace), Time
	 * @throws VpSemanticException
	 *             if no AnropsBehorighet is found
	 */
	public synchronized VisaVagvalResponse visaVagval(VisaVagvalRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent visaVagval");
		}

		// Dont force a reset, initialize only if needed
		// Guards against ongoing/failed init during startup 
		init(DONT_FORCE_RESET);

		if (!isInitialized()) {
			String errorMessage = "VP008 No contact with Tjanstekatalogen at startup, and no local cache to fallback on, not possible to route call";
			logger.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		// Determine if delimiter is set and present in request logical address.
		// Delimiter is used in deprecated default routing (VG#VE).
		boolean useDeprecatedDefaultRouting = addressDelimiter != null && addressDelimiter.length() > 0
				&& request.getReceiverId().contains(addressDelimiter);
		List<String> receiverAddresses = extractReceiverAdresses(request, useDeprecatedDefaultRouting);

		// Get possible routes (vagval)
		VisaVagvalResponse response = vagvalHandler.getRoutingInformation(request, useDeprecatedDefaultRouting, receiverAddresses);

		// No routing was found neither on requested receiver nor using the HSA
		// tree for parents. No need to continue to check authorization.
		if (vagvalHandler.containsNoRouting(response)) {
			return response;
		}

		// Check in TAK if sender is authorized to call the requested
		// receiver,if not check if sender is authorized to call any of the
		// receiver parents using HSA tree.
		//
		// Note: If old school default routing (VG#VE)HSA tree is used then we only get one address (the first one found routing info for) to check permissions for.
		if (!behorighetHandler.isAuthorized(request, receiverAddresses)) {
			throwNotAuthorizedException(request);
		}

		return response;
	}
	
	/**
	 * Read data without blocking for performance reasons.
	 */
	public int threadUnsafeLoadBalancerHealthCheckGetNumberOfVirtualizations() {
		return vagvalHandler != null ? vagvalHandler.getVirtualiseringsInfo().size() : 0;		
	}

	/**
	 * Read data without blocking for performance reasons.
	 */
	public int threadUnsafeLoadBalancerHealthCheckGetNumberOfAnropsBehorigheter() {
		return behorighetHandler != null ? behorighetHandler.getAnropsBehorighetsInfoList().size() : 0;		
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
}
