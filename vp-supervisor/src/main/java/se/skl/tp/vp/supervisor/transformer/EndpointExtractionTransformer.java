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

public class EndpointExtractionTransformer extends AbstractMessageTransformer {

    private static final Logger log = LoggerFactory.getLogger(EndpointExtractionTransformer.class);

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = message.getPayload(Map.class);
        URL endpointUrl = null;
        String endpoint = (String) map.get("serviceUrl");
        String producerId = ((Long)map.get("id")).toString();

        if (StringUtils.isBlank(endpoint)) {
            String msg = "Ignoring endpoint since it's blank";
            log.warn(msg);
            throw new RuntimeException(msg);
        }
        try {
            endpointUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            String msg = "Ignoring endpoint since it's illegal";
            log.warn(msg);
            throw new RuntimeException(msg);
        }

        if (logger.isDebugEnabled()) {
            log.debug("doTransform(" + this.getClass().getSimpleName() + ", " + encoding + ") returns: " + message);
        }

        message.setProperty("host", endpointUrl.getHost(), PropertyScope.OUTBOUND);
        message.setProperty("port", endpointUrl.getPort(), PropertyScope.OUTBOUND);
        message.setProperty("path", endpointUrl.getPath(), PropertyScope.OUTBOUND);
        message.setProperty("producerId", producerId, PropertyScope.OUTBOUND);

        return message;
    }
    
}
