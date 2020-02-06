package se.skl.tp.vp.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@Service
@Slf4j
public class HandleEmptyResponseProcessorImpl implements HandleEmptyResponseProcessor {

    ExceptionUtil exceptionUtil;

    @Autowired
    public HandleEmptyResponseProcessorImpl(ExceptionUtil exceptionUtil){
        this.exceptionUtil = exceptionUtil;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        try {
            Integer httpResponseCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            String strPayload = exchange.getIn().getBody(String.class);
            if (strPayload == null || strPayload.length() == 0 ) {
                log.debug("Found return message with length 0, replace with SoapFault because CXF doesn't like the empty string");
                String addr = exchange.getProperty(VPExchangeProperties.VAGVAL, "<UNKNOWN>", String.class);
                String cause = exceptionUtil.createMessage(VpSemanticErrorCodeEnum.VP009,
                    addr + ". Empty message when server responded with status code: " + SoapFaultHelper
                        .getStatusMessage(httpResponseCode.toString(), "NULL"));
                SoapFaultHelper.setSoapFaultInResponse(exchange, cause, VpSemanticErrorCodeEnum.VP009.toString());
            }
        } catch (Exception e) {
            log.error("An error occured in CheckPayloadTransformer!.", e);
        }
    }

}
