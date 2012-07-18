package se.skl.tp.vp.util.helper;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.VPUtil;

/**
 * Helper class for working with the
 * payload of messages
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class PayloadHelper extends VPHelperSupport {
	
	public PayloadHelper(MuleMessage muleMessage) {
		super(muleMessage, null, null);
	}
	
	/**
	 * Get the receiver from the payload.
	 * 
	 * @return the receiver or null if payload can't be parsed.
	 */
	public String extractReceiverFromPayload() {
		Object payload = getMuleMessage().getPayload();
		if (!(payload instanceof ReversibleXMLStreamReader)) {
			return null;
		}
		ReversibleXMLStreamReader reader = (ReversibleXMLStreamReader) payload;
		
		// Start caching events from the XML documents
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug("Start caching events from the XML docuement parsing");
		}
		
		reader.setTracking(true);
		
		try {
			return this.parsePayloadForReceiver(reader);
		} catch (final XMLStreamException e) {
			throw new VpTechnicalException(e);
		} finally {
			// Go back to the beginning of the XML document
			if (this.getLog().isDebugEnabled()) {
				this.getLog().debug("Go back to the beginning of the XML document");
			}
			reader.reset();
		}
	}

	private String parsePayloadForReceiver(final ReversibleXMLStreamReader reader) throws XMLStreamException {
		final String rivVersion = this.getRivVersion();
		
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
						if (this.getLog().isDebugEnabled()) {
							this.getLog().debug("found To in Header= " + receiverId);
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
					if (this.getLog().isDebugEnabled()) {
						this.getLog().debug("We have found the end element of the Header, i.e. we are done.");
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
	
	//
	private String getRivVersion() {
		return (String) this.getMuleMessage().getProperty(VPUtil.RIV_VERSION, PropertyScope.SESSION);
	}

}