package se.skl.tp.vp.timeout;

import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.constants.VPExchangeProperties;

import static org.apache.camel.component.netty.NettyConstants.NETTY_REQUEST_TIMEOUT;

@Service
public class RequestTimoutProcessorImpl implements RequestTimeoutProcessor {

    TimeoutConfiguration timeoutConfiguration;
    private final String DEFAULT_TJANSTEKONTRAKT;

    @Autowired
    public RequestTimoutProcessorImpl(TimeoutConfiguration timeoutConfiguration,
                                      @Value("${" + PropertyConstants.TIMEOUT_JSON_FILE_DEFAULT_TJANSTEKONTRAKT_NAME + "}") String default_tjanstekontrakt) {
        this.timeoutConfiguration = timeoutConfiguration;
        this.DEFAULT_TJANSTEKONTRAKT = default_tjanstekontrakt;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        TimeoutConfig timoutConfig = timeoutConfiguration.getOnTjanstekontrakt(exchange.getProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, String.class));
        if(timoutConfig != null) {
            exchange.getIn().setHeader(NETTY_REQUEST_TIMEOUT, timoutConfig.getProducertimeout());
        } else {
            TimeoutConfig defaultConfig = timeoutConfiguration.getOnTjanstekontrakt(DEFAULT_TJANSTEKONTRAKT);
            exchange.getIn().setHeader(NETTY_REQUEST_TIMEOUT, defaultConfig.getProducertimeout());
        }

    }
}
