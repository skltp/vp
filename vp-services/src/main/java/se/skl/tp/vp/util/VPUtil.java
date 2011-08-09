package se.skl.tp.vp.util;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mule.api.MuleMessage;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;

public final class VPUtil {

	private static Logger log = LoggerFactory.getLogger(VPUtil.class);
	
	/**
	 * Returns the elements from the RIV Header that are required by the
	 * VagvalAgent.
	 * 
	 * @param message
	 * @return
	 */
	public static String getReceiverId(MuleMessage message) {

		Object payload = message.getPayload();
		//DepthXMLStreamReader dxsr = (DepthXMLStreamReader) payload;
		ReversibleXMLStreamReader rxsr = (ReversibleXMLStreamReader) payload;//dxsr.getReader();

		// Start caching events from the XML documents
		if (log.isDebugEnabled()) {
			log.debug("Start caching events from the XML docuement parsing");
		}
		rxsr.setTracking(true);

		try {

			return doGetReceiverIdFromPayload(rxsr);

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
	private static String doGetReceiverIdFromPayload(XMLStreamReader reader)
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

				if (local.equals("To") && headerFound) {
					reader.next();
					receiverId = reader.getText();
					if (log.isDebugEnabled()) {
						log.debug("found To in Header= " + receiverId);
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
}
