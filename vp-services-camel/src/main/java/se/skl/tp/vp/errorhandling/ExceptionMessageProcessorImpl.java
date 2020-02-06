package se.skl.tp.vp.errorhandling;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
@Log4j2
public class ExceptionMessageProcessorImpl implements ExceptionMessageProcessor {

  public static final String DEFAULT_ERROR_CODE = "VP009";

  @Override
  public void process(Exchange exchange) throws Exception {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    String message = throwable.getMessage();
    SoapFaultHelper.setSoapFaultInResponse(exchange, message, getErrorCode(throwable));
    exchange.getIn().setHeader("Content-Type", "text/xml");

    log.debug("Error logged. Cause:" + message);
  }

  private String getErrorCode(Throwable throwable) {
    if (throwable instanceof VpSemanticException) {
      return ((VpSemanticException) throwable).getErrorCode().toString();
    }
    return DEFAULT_ERROR_CODE;
  }


}
