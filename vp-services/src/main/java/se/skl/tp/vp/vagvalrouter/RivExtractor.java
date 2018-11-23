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
package se.skl.tp.vp.vagvalrouter;

import javax.xml.namespace.QName;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.ExecutionTimer;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.PayloadHelper;
import se.skl.tp.vp.util.helper.PayloadHelper.PayloadInfo;

public class RivExtractor extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(RivExtractor.class);
	
	//
	public RivExtractor() {
		super();
	}
	
	@Override
	public Object transformMessage(MuleMessage msg, String encoding)
			throws TransformerException {
		
		log.debug("Extracting RIV-version, receiverId, wsdl namespace and servicecontract namespace");
		
		// open timers, and start total timer.
		ExecutionTimer.init();
		ExecutionTimer.start(VPUtil.TIMER_TOTAL);
		
		/*
		 * cxf_service is set by cxf:proxy_service on inbound.
		 */
		QName qname = msg.getProperty(VPUtil.CXF_SERVICE_NAMESPACE, PropertyScope.INVOCATION);
		final String tns = VPUtil.extractNamespaceFromService(qname);

		if (tns != null) {
			final String[] split = tns.split(":");

			final String rivVersion = split[split.length - 1].toUpperCase();

			log.debug("RIV-version set to sessions scope: " + rivVersion);
			msg.setProperty(VPUtil.RIV_VERSION, rivVersion, PropertyScope.SESSION);

			log.debug("WSDL namespace set to session scope: " + tns);
			msg.setProperty(VPUtil.WSDL_NAMESPACE, tns, PropertyScope.SESSION);
		} else {
			log.warn("No CXF service namespace in invocation scope");			
		}
		
		final PayloadHelper payloadHelper = new PayloadHelper(msg);
		PayloadInfo payloadInfo = payloadHelper.extractInfoFromPayload();
		
		if(payloadInfo.getServiceContractNamespace() != null){
			msg.setProperty(VPUtil.SERVICECONTRACT_NAMESPACE, payloadInfo.getServiceContractNamespace(), PropertyScope.SESSION);
		}else{
			log.warn("Service contract namespace was not found in the request");
		}
		
		if (payloadInfo.getReceiverId() != null) {
			log.debug("Receiver id (route to) set to session scope: " + payloadInfo.getReceiverId());
			msg.setProperty(VPUtil.RECEIVER_ID, payloadInfo.getReceiverId(), PropertyScope.SESSION);
		} else {
			log.warn("Unable to extract receiverid from paylaod");			
		}

		return msg;
	}

}
