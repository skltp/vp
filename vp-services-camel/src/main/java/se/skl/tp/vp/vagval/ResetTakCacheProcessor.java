package se.skl.tp.vp.vagval;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.TakCacheLog;

@Component
public class ResetTakCacheProcessor implements Processor {

    private final TakCacheService takService;

    @Autowired
    public ResetTakCacheProcessor(TakCacheService takService) {
        this.takService = takService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        TakCacheLog result = takService.refresh();
        exchange.getMessage().setBody(getResultAsString(result));
        exchange.getMessage().getHeaders().clear();
        exchange.getMessage().setHeader("Content-Type", "text/html;");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    }

    private String getResultAsString(TakCacheLog result) {
        StringBuilder resultAsString = new StringBuilder();
        for (String processingLog : result.getLog()) {
            resultAsString.append("<br>").append(processingLog);
        }
        return resultAsString.toString();
    }
}
