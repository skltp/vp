package se.skl.tp.vp.vagvalrouter.producer;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.vagvalrouter.VagvalRouter;

public class VpTestProducerLogger extends AbstractMessageTransformer {

	private static final Logger log = LoggerFactory.getLogger(VpTestProducerLogger.class);

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		@SuppressWarnings("unchecked")
		Map<String, Object> httpHeaders = (Map<String, Object>)message.getInboundProperty("http.headers");
		
		Object orgConsumer = httpHeaders.get(VagvalRouter.X_VP_CONSUMER_ID);

		log.info("Test producer called with {}: {}", VagvalRouter.X_VP_CONSUMER_ID, orgConsumer);

		return message;
	}

}
