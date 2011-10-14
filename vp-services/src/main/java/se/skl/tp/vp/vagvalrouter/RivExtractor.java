package se.skl.tp.vp.vagvalrouter;

import javax.xml.namespace.QName;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.VPUtil;

public class RivExtractor extends AbstractMessageAwareTransformer {

	private static final Logger log = LoggerFactory.getLogger(RivExtractor.class);
	
	@Override
	public Object transform(MuleMessage msg, String arg1)
			throws TransformerException {
		
		log.debug("Extracting RIV-version");
		
		final String tns = VPUtil.extractNamespaceFromService((QName) msg.getProperty(VPUtil.SERVICE_NAMESPACE));
		final String[] split = tns.split(":");
		
		final String rivVersion = split[split.length - 1];
	
		log.debug("RIV-version set to: " + rivVersion);
		msg.setProperty(VPUtil.RIV_VERSION, rivVersion.toUpperCase());
		
		return msg;
	}

}
