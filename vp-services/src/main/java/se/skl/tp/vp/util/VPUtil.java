package se.skl.tp.vp.util;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mule.api.MuleMessage;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

public final class VPUtil {

	private static Logger log = LoggerFactory.getLogger(VPUtil.class);
	
	public static String RECEIVER_ID = "receiverid";
	public static String SENDER_ID = "senderid";
	public static String RIV_VERSION = "rivversion";
	public static String SERVICE_NAMESPACE = "cxf_service";
	
	public static String extractNamespaceFromService(final QName qname) {
		return qname.getNamespaceURI();
	}
	
	/**
	 * Returns the elements from the RIV Header that are required by the
	 * VagvalAgent.
	 * 
	 * @param message
	 * @return
	 */
	public static String getReceiverId(MuleMessage message) {

		Object payload = message.getPayload();
		ReversibleXMLStreamReader rxsr = (ReversibleXMLStreamReader) payload;

		final String rivVersion = (String) message.getProperty(VPUtil.RIV_VERSION);
		
		// Start caching events from the XML documents
		if (log.isDebugEnabled()) {
			log.debug("Start caching events from the XML docuement parsing");
		}
		rxsr.setTracking(true);

		try {

			return doGetReceiverIdFromPayload(rxsr, rivVersion);

		} catch (XMLStreamException e) {
			throw new VpTechnicalException(e);

		} finally {
			// Go back to the beginning of the XML document
			if (log.isDebugEnabled()) {
				log.debug("Go back to the beginning of the XML document");
			}
			rxsr.reset();
		}
	}
	
	public static String getSenderIdFromCertificate(MuleMessage message, final Pattern pattern) {

		String senderId = null;
		Certificate[] peerCertificateChain = (Certificate[]) message
				.getProperty("PEER_CERTIFICATES");

		if (peerCertificateChain != null) {
			// Check type of first certificate in the chain, this should be the
			// clients certificate
			if (peerCertificateChain[0] instanceof X509Certificate) {
				X509Certificate cert = (X509Certificate) peerCertificateChain[0];
				String principalName = cert.getSubjectX500Principal().getName();
				Matcher matcher = pattern.matcher(principalName);
				if (matcher.find()) {
					senderId = matcher.group(1);
				} else {
					String errorMessage = ("VP002 No senderId found in Certificate: " + principalName);
					log.info(errorMessage);
					throw new VpSemanticException(errorMessage);

				}
			} else {
				String errorMessage = ("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate: " + peerCertificateChain[0]);
				log.info(errorMessage);
				throw new VpSemanticException(errorMessage);
			}
		} else {
			String errorMessage = ("VP002 No senderId found in Certificate: No certificate chain found from client");
			log.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		
		// Check if this is coded in hex (HCC Funktionscertifikat does that!)
		if (senderId.startsWith("#")) {
			return convertFromHexToString(senderId.substring(5));
		} else {
			return senderId;			
		}
	}
	
	/**
	 * Uses the StAX - API to get the elements from the SOAP Header.
	 * 
	 * @param reader
	 * @return
	 * @throws XMLStreamException
	 */
	private static String doGetReceiverIdFromPayload(final XMLStreamReader reader, final String rivVersion)
			throws XMLStreamException {

		String receiverId = null;
		boolean headerFound = false;

		int event = reader.getEventType();

		while (reader.hasNext()) {
			switch (event) {
			
			case XMLStreamConstants.START_ELEMENT:
				String local = reader.getLocalName();

				if (local.equals("Header")) {
					headerFound = true;
				}

				if (rivVersion.equals("RIVTABP20")) {
					if (local.equals("To") && headerFound) {
						reader.next();
						receiverId = reader.getText();
						if (log.isDebugEnabled()) {
							log.debug("found To in Header= " + receiverId);
						}
					}
				}
				
				if (rivVersion.equals("RIVTABP21")) {
					if (local.equals("LogicalAddress") && headerFound) {
						reader.next();
						receiverId = reader.getText();
					}
				}

				break;

			case XMLStreamConstants.END_ELEMENT:
				if (reader.getLocalName().equals("Header")) {
					// We have found the end element of the Header, i.e. we
					// are done. Let's bail out!
					if (log.isDebugEnabled()) {
						log.debug("We have found the end element of the Header, i.e. we are done.");
					}
					return receiverId;
				}
				break;

			case XMLStreamConstants.CHARACTERS:
				break;

			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.END_DOCUMENT:
			case XMLStreamConstants.ATTRIBUTE:
			case XMLStreamConstants.NAMESPACE:
				break;

			default:
				break;
			}
			event = reader.next();
		}

		return receiverId;
	}
	
	private static String convertFromHexToString(final String hexString) {
		byte [] txtInByte = new byte [hexString.length() / 2];
		int j = 0;
		for (int i = 0; i < hexString.length(); i += 2)
		{
			txtInByte[j++] = Byte.parseByte(hexString.substring(i, i + 2), 16);
		}
		return new String(txtInByte);
	}
	
	public static VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId,
			String tjansteGranssnitt) {
		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);

		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		vvR.setTidpunkt(tidPunkt);

		return vvR;
	}
	
	public static VisaVagvalRequest createVisaVagvalRequest(final VagvalInput input) {
		return VPUtil.createVisaVagvalRequest(input.senderId, input.receiverId, input.serviceNamespace);
	}
	
	public static VagvalInput createRequestToServiceDirectory(final MuleMessage msg, final Pattern pattern) {
		VagvalInput vagvalInput = new VagvalInput();

		vagvalInput.senderId = VPUtil.getSenderIdFromCertificate(msg, pattern);
		msg.setProperty(VPUtil.SENDER_ID, vagvalInput.senderId);

		vagvalInput.receiverId = (String) msg.getProperty(VPUtil.RECEIVER_ID);
		vagvalInput.rivVersion = (String) msg.getProperty(VPUtil.RIV_VERSION);
		vagvalInput.serviceNamespace = VPUtil.extractNamespaceFromService((QName) msg.getProperty(VPUtil.SERVICE_NAMESPACE));
		
		return vagvalInput;
	}
	
	public static void verifyRequestToServiceDirectory(final VagvalInput vagvalInput) {
		if (log.isDebugEnabled()) {
			log.debug("Calling vagvalAgent with serviceNamespace:"
					+ vagvalInput.serviceNamespace + ", receiverId:" + vagvalInput.receiverId
					+ ", senderId: " + vagvalInput.senderId);
		}
		if (vagvalInput.rivVersion == null) {
			String errorMessage = ("VP001 No Riv-version configured in tp-virtuell-tjanst-config, transformer-refs with property name 'rivversion' missing");
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (vagvalInput.senderId == null) {
			String errorMessage = ("VP002 No senderId found in Certificate");
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (vagvalInput.receiverId == null) {
			String errorMessage = ("VP003 No receiverId found in RivHeader");
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
	}
	
	/**
	 * Get all virtualized services from a
	 * response.
	 * @param response
	 * @return
	 */
	public static List<VirtualiseringsInfoType> getVirtualizedServices(final VagvalInput input, final VisaVagvalResponse response) {
		List<VirtualiseringsInfoType> virtualiseringar = response.getVirtualiseringsInfo();
		if (log.isDebugEnabled()) {
			log.debug("VagvalAgent response count: " + virtualiseringar.size());
			for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
				log.debug("VagvalAgent response item RivProfil: " + vvInfo.getRivProfil()
						+ ", Address: " + vvInfo.getAdress());
			}
		}
		
		if (virtualiseringar.size() == 0) {
			String errorMessage = "VP004 No Logical Adress found for serviceNamespace:"
					+ input.serviceNamespace + ", receiverId:" + input.receiverId;
			log.info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		
		return virtualiseringar;
	}
	
	public static String getAddressToVirtualizedService(final VagvalInput input, final List<VirtualiseringsInfoType> virtualizedServices) {
		String adress = null;
		int noOfMatchingAdresses = 0;
		for (VirtualiseringsInfoType vvInfo : virtualizedServices) {
			if (vvInfo.getRivProfil().equals(input.rivVersion)) {
				adress = vvInfo.getAdress();
				noOfMatchingAdresses++;
			}
		}

		if (noOfMatchingAdresses == 0) {
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ input.serviceNamespace
					+ ", receiverId:"
					+ input.receiverId
					+ "RivVersion" + input.rivVersion);
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (noOfMatchingAdresses > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ input.serviceNamespace + ", receiverId:" + input.receiverId;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = ("VP010 Physical Adress field is empty in Service Producer for serviceNamespace :"
					+ input.serviceNamespace
					+ ", receiverId:"
					+ input.receiverId
					+ "RivVersion" + input.rivVersion);
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		adress = "cxf:" + adress;

		return adress;
	}
	
	public static String getAvailableRivProfile(final VisaVagvalsInterface agent, final MuleMessage msg, final Pattern pattern) {
		
		final VagvalInput input = VPUtil.createRequestToServiceDirectory(msg, pattern);
		final VisaVagvalResponse response = agent.visaVagval(VPUtil.createVisaVagvalRequest(input));
		
		final List<VirtualiseringsInfoType> virts = VPUtil.getVirtualizedServices(input, response);
		
		final Set<String> rivProfiles = new HashSet<String>();
		for (final VirtualiseringsInfoType virt : virts) {
			rivProfiles.add(virt.getRivProfil());
		}
		
		if (rivProfiles.size() == 0) {
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ input.serviceNamespace
					+ ", receiverId:"
					+ input.receiverId
					+ "RivVersion" + input.rivVersion);
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (rivProfiles.size() > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ input.serviceNamespace + ", receiverId:" + input.receiverId;
			log.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		
		return rivProfiles.iterator().next();
	}
}
