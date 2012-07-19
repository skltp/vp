package se.skl.tp.vp.util;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;

/**
 * Logs error events on any kind of exception, and should be used for all VP services.
 * 
 * @author Peter
 * @since VP-2.0
 */
public class VPExceptionStrategy extends DefaultMessagingExceptionStrategy  {
	private static final Logger log = LoggerFactory.getLogger(VPExceptionStrategy.class);
	
	private final EventLogger eventLogger;

	public VPExceptionStrategy(MuleContext muleContext) {
		super(muleContext);
		this.eventLogger = new EventLogger(muleContext);
	}

	/**
	 * Setter for the jaxbToXml property
	 * 
	 * @param jaxbToXml
	 */
	public void setJaxbObjectToXml(JaxbObjectToXmlTransformer jaxbToXml) {
		this.eventLogger.setJaxbToXml(jaxbToXml);
	}

	@Override
	protected void logException(Throwable t) {
                
		log.debug("Entering VPExceptionStrategy...");
		
		MuleException muleException = ExceptionHelper.getRootMuleException(t);
        if (muleException != null) {

        	if (muleException instanceof MessagingException) {
        		MessagingException me = (MessagingException)muleException;
            	
        		MuleMessage msg = me.getEvent().getMessage();
            	
        		Throwable ex = (me.getCause() == null ? me : me.getCause());
        		
        		msg.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);
        		msg.setProperty(VPUtil.SESSION_ERROR_DESCRIPTION, ex.getLocalizedMessage(), PropertyScope.SESSION);
        		msg.setProperty(VPUtil.SESSION_ERROR_TECHNICAL_DESCRIPTION, ex.toString(), PropertyScope.SESSION);
            	msg.setProperty(VPUtil.SESSION_ERROR, Boolean.TRUE, PropertyScope.SESSION);

        		Map<String, String> extraInfo = new HashMap<String, String>();
    			extraInfo.put("source", getClass().getName());
        		eventLogger.addSessionInfo(msg, extraInfo);
        		eventLogger.logErrorEvent(ex, msg, null, extraInfo); 

        	} else {
                @SuppressWarnings("unchecked")
				Map<String, Object> info = ExceptionHelper.getExceptionInfo(muleException);
        		eventLogger.logErrorEvent(muleException, info.get("Payload"), null, null);                
        	}
        	
        } else {
    		eventLogger.logErrorEvent(t, null, null, null);
        }
	}
}
