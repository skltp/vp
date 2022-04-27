package se.skl.tp.vp.vagval;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.service.HsaCacheService;

@Component
public class ResetHsaCacheProcessor implements Processor {

    HsaCacheService hsaCacheService;

    @Autowired
    public ResetHsaCacheProcessor(HsaCacheService hsaCacheService) {
        this.hsaCacheService=hsaCacheService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String result = hsaCacheService.resetCache();
        exchange.getMessage().getHeaders().clear();
        exchange.getMessage().setBody(result);
    }
}

