package se.skl.tp.vp.logmanager;

import javax.jms.JMSException;
import javax.resource.spi.IllegalStateException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.util.ByteArrayInputStream;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;

public class JmsToLogEventTransformer extends AbstractTransformer {

	private static final Logger log = LoggerFactory.getLogger(JmsToLogEventTransformer.class);
	
	@Override
	protected Object doTransform(Object arg0, String arg1)
			throws TransformerException {
		
		log.debug("Transforming {} to log event object", arg0.getClass().getName());
		
		if (arg0 instanceof ActiveMQTextMessage) {
			
			final ActiveMQTextMessage msg = (ActiveMQTextMessage) arg0;
			
			try {
				final JAXBContext jc = JAXBContext.newInstance(LogEvent.class);
				final LogEvent le = (LogEvent) jc.createUnmarshaller().unmarshal(new ByteArrayInputStream(msg.getText().getBytes()));
				
				return le;
			} catch (JMSException e) {
				throw new TransformerException(this, e);
			} catch (JAXBException e) {
				throw new TransformerException(this, e);
			}
		}
		
		throw new TransformerException(this, new IllegalStateException("Object to transform is not an ActiveMQTextMessage but was: {}", arg0.getClass().getName()));
	}

}
