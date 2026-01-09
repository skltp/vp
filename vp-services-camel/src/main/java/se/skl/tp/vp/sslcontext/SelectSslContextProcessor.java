package se.skl.tp.vp.sslcontext;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;

@Service
public class SelectSslContextProcessor implements Processor {
    SSLContextService sslContextService;
    public SelectSslContextProcessor(SSLContextService sslContextService) {
        this.sslContextService = sslContextService;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        String vagvalHost = exchange.getProperty(VPExchangeProperties.VAGVAL_HOST, String.class);
        if (vagvalHost == null) {
            throw new IllegalStateException("Vagval not set in exchange properties");
        }
        String sslContextId = sslContextService.getClientSSLContextId(vagvalHost);
        exchange.setProperty(VPExchangeProperties.SSL_CONTEXT_ID, sslContextId);
    }
}
