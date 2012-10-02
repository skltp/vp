package se.skl.tp.vp.supervisor.transformer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts producer Endpoint from payload.
 * 
 * @since VP-2.0
 * @author Anders
 *
 */
public class EndpointExtractionTransformer extends AbstractMessageTransformer {

    private static final Logger log = LoggerFactory.getLogger(EndpointExtractionTransformer.class);

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = message.getPayload(Map.class);
        String endpoint = (String) map.get("serviceUrl");

        URL endpointUrl = toURL(endpoint);

        if (logger.isDebugEnabled()) {
            log.debug("doTransform(" + this.getClass().getSimpleName() + ", " + encoding + ") returns: " + message);
        }

        //
        String producerId = ((Long)map.get("id")).toString();
        message.setProperty("producerId", producerId, PropertyScope.SESSION);
        message.setProperty("serviceUrl", endpoint, PropertyScope.OUTBOUND);

        // FIXME: Legacy stuff
        message.setProperty("protocol", endpointUrl.getProtocol(), PropertyScope.OUTBOUND);
        message.setProperty("host", endpointUrl.getHost(), PropertyScope.OUTBOUND);
        if (endpointUrl.getPort() > 0) {
        	message.setProperty("port", ":" + endpointUrl.getPort(), PropertyScope.OUTBOUND);
        } else {
        	message.setProperty("port", "", PropertyScope.OUTBOUND);
        }
        message.setProperty("path", endpointUrl.getPath(), PropertyScope.OUTBOUND);


        return message;
    }
    
    //
    private static URL toURL(String endpoint) {
        try {
        	if (StringUtils.isBlank(endpoint)) {
        		throw new MalformedURLException("No endpoint defined (empty string)");
        	}
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            String msg = "Ignoring endpoint since it's illegal";
            log.error(msg);
            throw new RuntimeException(msg, e);
        }    	
    }
}
