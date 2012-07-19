package se.skl.tp.vp.supervisor.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import riv.itintegration.monitoring.pingforconfigurationresponder._1.*;

/**
 * Creates a PingforConfiguration request payload..
 * 
 * @since VP-2.0
 * @author Anders
 */
public class PingForConfigurationTypeTransformer extends AbstractTransformer {

	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationTypeTransformer.class);

	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {

		PingForConfigurationType type = new PingForConfigurationType();

		if (logger.isDebugEnabled()) {
			log.debug("doTransform(" + src.getClass().getSimpleName() + ", " + encoding + ") returns: " + type);
		}

		return new Object[] { "SE165565594230-1000", type };
	}
}
