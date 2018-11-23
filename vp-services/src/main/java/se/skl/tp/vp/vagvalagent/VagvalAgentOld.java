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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import org.python.icu.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.ClientUtil;
import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.MessageProperties;
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
import se.skltp.takcache.RoutingInfo;

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

public class VagvalAgentOld implements VisaVagvalsInterface {

	private static final Logger logger = LoggerFactory.getLogger(VagvalAgentOld.class);
	public static final boolean FORCE_RESET = true;
	public static final boolean DONT_FORCE_RESET = false;

	private static InetAddress HOST = null;
	private static String HOST_NAME = "UNKNOWN";
	private static Calendar calendar = Calendar.getInstance();

		/**
	 * Persistent cache. 
	 */
	@XmlRootElement
	static class PersistentCache implements Serializable {
		private static final long serialVersionUID = 1L;
		@XmlElement
		private List<VirtualiseringsInfoType> virtualiseringsInfo;
		@XmlElement
		private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;
	}

	/**
	 * Runtime cache. 
	 */
	class TakCache {
		VagvalHandler vagvalHandler;
		BehorighetHandler behorighetHandler;
	}
	
	private static final JaxbUtil JAXB = new JaxbUtil(PersistentCache.class);

	private String localTakCache;
	private Boolean removeLocalCacheAllowed;

	private TakCache takCache;
	private boolean takCacheIsInitialized = false;
	
	private HsaCache hsaCache;

	private String endpointAddressTjanstekatalog;
	private String addressDelimiter;
	
	private MessageProperties messageProperties;

	private SokVagvalsInfoInterface port = null;
	
	private Object lockTakFetch = new Object();

	private se.skl.tp.behorighet.BehorighetHandler behorighetHandler;
	private se.skl.tp.vagval.VagvalHandler vagvalHandler;


	public VagvalAgentOld() {
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
	
	public void setMessageProperties(MessageProperties messageProperties) {
		this.messageProperties = messageProperties;
	}

	public void setBehorighetHandler(se.skl.tp.behorighet.BehorighetHandler behorighetHandler) {
		this.behorighetHandler = behorighetHandler;
	}

	public void setVagvalHandler(se.skl.tp.vagval.VagvalHandler vagvalHandler) {
		this.vagvalHandler = vagvalHandler;
	}

	/* For unit tests only */
	public void setRemoveTakCacheAllowed(Boolean remove) {
		this.removeLocalCacheAllowed = remove;
	}
	
	/* For unit tests only */
	public void doRemoveTakCache() {
		if(this.removeLocalCacheAllowed != null && this.removeLocalCacheAllowed) {
			File file = new File(this.localTakCache);
			if(file.exists())
				file.delete();	
		}
	}
	

	/**
	 * Initialize VagvalAgentOld resources.
	 * If not forced, init checks if necessary resources are loaded, otherwise
	 * resources are loaded.
	 *
	 * @param forceReset force a reset/refresh using true, false for init. Use
	 * constants VagvalAgentOld.FORCE_RESET or VagvalAgentOld.DONT_FORCE_RESET.
	 * @return a processing log containing status for loading TAK resources
	 */
	public VagvalAgentProcessingLog init(boolean forceReset) {
		
		try {
			HOST       = InetAddress.getLocalHost();
			HOST_NAME  = HOST.getCanonicalHostName();
		} catch (UnknownHostException e) {

		}
		
		VagvalAgentProcessingLog processingLog = new VagvalAgentProcessingLog();
		if (!forceReset) {
			init(processingLog);
		}
		else {
			boolean isRefreshSuccessful = refresh(false, processingLog);
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
		if (!takCacheIsInitialized) {
			String logMsg = "init: not initialized, will do init ...";
			logger.info(logMsg);
			processingLog.addLog(logMsg);
			refresh(true, processingLog);
			logMsg = "init done, was successful: " + takCacheIsInitialized;
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
	private boolean refresh(boolean isInit, VagvalAgentProcessingLog processingLog) {

		boolean isRefreshSuccessful = false;
		// Use local df since not thread safe.
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		// only let one thread at a time attempt to fetch and persist TAK data
		synchronized (lockTakFetch) {

			logger.info("Initialize VagvalAgentOld TAK resources...");
			processingLog.addLog("Host: " + HOST_NAME);
			processingLog.addLog("Time: " + df.format(calendar.getTime()));
			processingLog.addLog("Initialize VagvalAgentOld TAK resources...");

			try {
				// both TAK calls to fetch data must succeed to have a consistent state
				List<VirtualiseringsInfoType> v = getVirtualiseringar();
				List<AnropsBehorighetsInfoType> p = getBehorigheter();
				
				if(v == null || v.size() == 0) {
					logger.warn("Failed to refresh TAK data. No VirtualiseringsInfo found!");					
				} else if(p == null || p.size() == 0) {
					logger.warn("Failed to refresh TAK data. No AnropsBehorighetsInfo found!");					
				} else {
					// do thread-safe update of cache
					updateTakCache(v, p);
					isRefreshSuccessful = true;
				}
			}
			catch (Exception e) {				
				logger.error("Failed to refresh TAK data", e);
			}

			if (isRefreshSuccessful) {
			    processingLog.addLog("Succeeded to get virtualizations and/or permissions from TAK, save to local TAK copy...");
				saveToLocalCopy(localTakCache, processingLog);
			} else if (isInit) {
				// try to load from local file cache
			    processingLog.addLog("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy...");
				restoreFromLocalCopy(localTakCache, processingLog);
			} else {
				// refresh failed but cache was already initialized
				processingLog.addLog("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Will continue to use already loaded TAK data.");				
			}

			if (isRefreshSuccessful || (isInit && takCacheIsInitialized)) {
				logger.info("Init VagvalAgentOld loaded number of permissions: {}", takCache.behorighetHandler.size());
				logger.info("Init VagvalAgentOld loaded number of virtualizations: {}", takCache.vagvalHandler.size());
				processingLog.addLog("Init VagvalAgentOld loaded number of permissions: " + takCache.behorighetHandler.size());
				processingLog.addLog("Init VagvalAgentOld loaded number of virtualizations: " + takCache.vagvalHandler.size());
			}

			logger.info("Init VagvalAgentOld done");
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
	private synchronized void updateTakCache(List<VirtualiseringsInfoType> v, List<AnropsBehorighetsInfoType> p) {
		TakCache takCache = new TakCache();
		takCache.vagvalHandler = new VagvalHandler(hsaCache, v);
		takCache.behorighetHandler = new BehorighetHandler(hsaCache, p);
		// minimize impact (cache reading is not locking/synchronized) when
		// cache is refreshed by setting the cache only when it is fully
		// populated and ready for use
		this.takCache = takCache;
		if (!takCacheIsInitialized) {
			takCacheIsInitialized = true;
		}
	}

	private SokVagvalsInfoInterface getPort() {
	    if(port == null){
	    	logger.info("Use TAK endpoint adress: {}", endpointAddressTjanstekatalog);
	        SokVagvalsServiceSoap11LitDocService service = new SokVagvalsServiceSoap11LitDocService(
	                ClientUtil.createEndpointUrlFromServiceAddress(endpointAddressTjanstekatalog));
	        port = service.getSokVagvalsSoap11LitDocPort();

	        /*
	         *  Set request headers
	         *  Note that this can not be configured in Mule since
	         *  call is made directly bypassing mule functionality.
	         */
	        Map<String, Object> req_ctx = ((BindingProvider)port).getRequestContext();
	        Map<String, List<String>> headers = new HashMap<String, List<String>>();
	        headers.put("User-Agent", Collections.singletonList(HttpHeaders.VP_HEADER_USER_AGENT));
	        req_ctx.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
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
			updateTakCache(pc.virtualiseringsInfo, pc.anropsBehorighetsInfo);
		}
	}

	// save object
	private void saveToLocalCopy(String fileName, VagvalAgentProcessingLog processingLog) {
		PersistentCache pc = new PersistentCache();
		pc.anropsBehorighetsInfo = takCache.behorighetHandler.getAnropsBehorighetsInfoList();
		pc.virtualiseringsInfo = takCache.vagvalHandler.getVirtualiseringsInfo();

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
	//public synchronized List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {
	public List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {
		if (!takCacheIsInitialized) {
			init(DONT_FORCE_RESET);	
		}
		return (takCache == null) ? Collections.<AnropsBehorighetsInfoType>emptyList() : takCache.behorighetHandler.getAnropsBehorighetsInfoList();
	}

	/**
	 * @deprecated since VP 2.10, should be removed when the webservice
	 * GetLogicalAddresseesByServiceContract is no longer produced by VP
	 *   
	 * Get routing information list from the internal TAK cache.
	 * @return list of routing information, empty if no routing information exists.
	 */
	@Deprecated
	//public synchronized List<VirtualiseringsInfoType> getVirtualiseringsInfo() {
	public List<VirtualiseringsInfoType> getVirtualiseringsInfo() {
		if (!takCacheIsInitialized) {
			init(DONT_FORCE_RESET);	
		}		
        return (takCache == null) ? Collections.<VirtualiseringsInfoType>emptyList(): takCache.vagvalHandler.getVirtualiseringsInfo();
    }

	/**
	 * Resets cache.
	 */
	public ResetVagvalCacheResponse resetVagvalCache(ResetVagvalCacheRequest parameters) {
		logger.info("Start force a reset of VagvalAgentOld...");

		ResetVagvalCacheResponse response = new ResetVagvalCacheResponse();

		//Force reset in init
        VagvalAgentProcessingLog processingLog = init(FORCE_RESET);

		if (!processingLog.isRefreshSuccessful) {
			response.setResetResult(false);
			logger.info("Failed force reset VagvalAgentOld");
		} else {
			response.setResetResult(true);
			logger.info("Successfully force reset VagvalAgentOld");
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
	public VisaVagvalResponse visaVagval(VisaVagvalRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent visaVagval");
		}

		// Dont force a reset, initialize only if needed
		// Guards against ongoing/failed init during startup
		// Note: try to not lock/synchronize during read, trade some thread-safety
		// against performance for the case when the cache is refreshed
		if (!takCacheIsInitialized) {
			init(DONT_FORCE_RESET);
		}
		

		if (!takCacheIsInitialized) {
			String errorMessage = messageProperties.get(VpSemanticErrorCodeEnum.VP008, null);
			logger.error(errorMessage);
			throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP008);
		}
		
		// hold a local copy of the TakCache to keep consistency during call if
		// cache is refreshed (since we don't synchronize read access to cache)
		TakCache takCacheForLocalMethodInvocation = this.takCache;

		// Determine if delimiter is set and present in request logical address.
		// Delimiter is used in deprecated default routing (VG#VE).
		boolean useDeprecatedDefaultRouting = false;
		if(request.getReceiverId() != null && addressDelimiter != null && addressDelimiter.length() > 0){
			useDeprecatedDefaultRouting = request.getReceiverId().contains(addressDelimiter);
		}

		List<String> receiverAddresses = extractReceiverAdresses(request, useDeprecatedDefaultRouting);

		// Get possible routes (vagval)
		VisaVagvalResponse response = takCacheForLocalMethodInvocation.vagvalHandler.getRoutingInformation(request, useDeprecatedDefaultRouting, receiverAddresses);

		// TODO test
		List<RoutingInfo> routingInfo = vagvalHandler.getRoutingInfo(request.getTjanstegranssnitt(), request.getReceiverId());
		List<VirtualiseringsInfoType> virtualiseringsInfo =  response.getVirtualiseringsInfo();
		compareResponses(routingInfo,virtualiseringsInfo);

		// No routing was found neither on requested receiver nor using the HSA
		// tree for parents. No need to continue to check authorization.
		if (takCacheForLocalMethodInvocation.vagvalHandler.containsNoRouting(response)) {
			return response;
		}

		// Check in TAK if sender is authorized to call the requested
		// receiver,if not check if sender is authorized to call any of the
		// receiver parents using HSA tree.
		//
		// Note: If old school default routing (VG#VE)HSA tree is used then we only get one address (the first one found routing info for) to check permissions for.

		// TODO test
		boolean newIsAuthorized = behorighetHandler.isAuthorized(request.getSenderId(), request.getTjanstegranssnitt(), request.getReceiverId());
		boolean oldIsAuthorized = takCacheForLocalMethodInvocation.behorighetHandler.isAuthorized(request, receiverAddresses);
		if (newIsAuthorized != oldIsAuthorized) {
			System.out.println("Test failure!");
			logger.error("Nytt sätt för behörighet!=original: newIsAuthorized:"+newIsAuthorized+" oldIsAuthorized:"+oldIsAuthorized);
		}
			if (!oldIsAuthorized) {
			throwNotAuthorizedException(request);
		}

		return response;
	}

	// TODO just test
	private void compareResponses(List<RoutingInfo> routingInfoList, List<VirtualiseringsInfoType> virtualiseringsInfos) {
		System.out.printf("routingInfo size: %d, virtualisering size: %d\n", routingInfoList.size(), virtualiseringsInfos.size());
		if (routingInfoList.size() != virtualiseringsInfos.size()) {
			System.out.println("Test failure!");
			logger.error("Nytt sätt för vägval!=original: routingInfoList.size():"+routingInfoList.size()+" virtualiseringsInfos.size():"+virtualiseringsInfos.size());
		} else{
			for(int i =0; i<routingInfoList.size(); i++){
				compareVagval(routingInfoList.get(i), virtualiseringsInfos.get(i));
			}
		}
		printVagval(routingInfoList, virtualiseringsInfos);
	}

	private void compareVagval(RoutingInfo routingInfo,
			VirtualiseringsInfoType virtualiseringsInfo) {
		if(routingInfo.getAddress() == null ){
			if(virtualiseringsInfo.getAdress()!=null){
				System.out.println("Test failure!");
				logger.error("Nytt sätt för vägval!=original: new:null old:"+virtualiseringsInfo.getAdress());
			}
		} else if(!routingInfo.getAddress().equals(virtualiseringsInfo.getAdress())){
			System.out.println("Test failure!");
			logger.error("Nytt sätt för vägval!=original: new:"+routingInfo.getAddress()+" old:"+virtualiseringsInfo.getAdress());
		}

		if(routingInfo.getRivProfile()==null){
			if(virtualiseringsInfo.getRivProfil()!=null){
				System.out.println("Test failure!");
				logger.error("Nytt sätt för vägval!=original: new:null old:"+virtualiseringsInfo.getRivProfil());
			}
		}else	if(!routingInfo.getRivProfile().equals(virtualiseringsInfo.getRivProfil())){
			logger.error("Nytt sätt för vägval!=original: new:"+routingInfo.getRivProfile()+" old:"+virtualiseringsInfo.getRivProfil());
		}
	}

	// TODO just test
	private void printVagval(List<RoutingInfo> routingInfoList,
			List<VirtualiseringsInfoType> virtualiseringsInfos) {
		for(RoutingInfo routingInfo: routingInfoList){
			System.out.printf("RoutingInfo address: %s, RoutingInfo profile: %s \n", routingInfo.getAddress(), routingInfo.getRivProfile());
		}
		for(VirtualiseringsInfoType virtualiseringsInfoType: virtualiseringsInfos){
			System.out.printf("virtualiseringsInfoType address: %s, virtualiseringsInfoType profile: %s \n", virtualiseringsInfoType.getAdress(), virtualiseringsInfoType.getRivProfil());
		}
	}

	/**
	 * Read data without blocking for performance reasons.
	 */
	public int threadUnsafeLoadBalancerHealthCheckGetNumberOfVirtualizations() {
		return takCache != null ? takCache.vagvalHandler.getVirtualiseringsInfo().size() : 0;		
	}

	/**
	 * Read data without blocking for performance reasons.
	 */
	public int threadUnsafeLoadBalancerHealthCheckGetNumberOfAnropsBehorigheter() {
		return takCache != null ? takCache.behorighetHandler.getAnropsBehorighetsInfoList().size() : 0;		
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
		String errorMessage = messageProperties.get(VpSemanticErrorCodeEnum.VP007,getRequestSummary(request));
		logger.info(errorMessage);
		throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP007);
	}
	
	private String getRequestSummary(VisaVagvalRequest request) {
		return 
			"serviceNamespace: " + request.getTjanstegranssnitt()
			+ ", receiverId: " + request.getReceiverId() 
			+ ", senderId: " + request.getSenderId();
	}
}
