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

public class RivExtractor extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(RivExtractor.class);
	
	//
	public RivExtractor() {
		super();
	}
	
	@Override
	public Object transformMessage(MuleMessage msg, String encoding)
			throws TransformerException {
		
		log.debug("Extracting RIV-version and namespace");
		
		// open timers, and start total timer.
		ExecutionTimer.init();
		ExecutionTimer.start(VPUtil.TIMER_TOTAL);
		
	
		QName qname = (QName) msg.getProperty(VPUtil.SERVICE_NAMESPACE, PropertyScope.INVOCATION);
		final String tns = VPUtil.extractNamespaceFromService(qname);

		if (tns != null) {
			final String[] split = tns.split(":");

			final String rivVersion = split[split.length - 1].toUpperCase();

			log.debug("RIV-version set to sessions scope: " + rivVersion);
			msg.setProperty(VPUtil.RIV_VERSION, rivVersion, PropertyScope.SESSION);

			log.debug("Service namespave set to session scope: " + tns);
			msg.setProperty(VPUtil.SERVICE_NAMESPACE, tns, PropertyScope.SESSION);
		} else {
			log.warn("No service namespace in invocation scope");			
		}
		
		final PayloadHelper payloadHelper = new PayloadHelper(msg);
		final String receiverId = payloadHelper.extractReceiverFromPayload();
		if (receiverId != null) {
			log.debug("Receiver id (route to) set to session scope: " + receiverId);
			msg.setProperty(VPUtil.RECEIVER_ID, receiverId, PropertyScope.SESSION);
		} else {
			log.warn("Unable to extract receiverid from paylaod");			
		}

		return msg;
	}

}
