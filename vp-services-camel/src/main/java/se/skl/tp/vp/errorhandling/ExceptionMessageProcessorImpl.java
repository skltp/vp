package se.skl.tp.vp.errorhandling;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
@Log4j2
public class ExceptionMessageProcessorImpl implements ExceptionMessageProcessor {

  @Override
  public void process(Exchange exchange) throws Exception {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

    VpSemanticErrorCodeEnum errorCodeEnum = VpSemanticErrorCodeEnum.getDefault();
    String message = throwable.getMessage();
    String messageDetails = "";

    if (throwable instanceof VpSemanticException) {
      VpSemanticException exception = (VpSemanticException) throwable;
      messageDetails = exception.getMessageDetails();
      errorCodeEnum = exception.getErrorCode();
    }

    SoapFaultHelper.setSoapFaultInResponse(exchange, message, messageDetails, errorCodeEnum);
    exchange.getIn().setHeader("Content-Type", "text/xml");

    log.debug("Error logged. Cause:" + message);
  }

}
