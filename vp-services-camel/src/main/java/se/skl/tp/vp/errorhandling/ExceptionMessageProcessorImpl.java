package se.skl.tp.vp.errorhandling;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.exceptions.VpRuntimeException;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
@Log4j2
public class ExceptionMessageProcessorImpl implements ExceptionMessageProcessor {

  private final ExceptionUtil exceptionUtil;

  @Autowired
  public ExceptionMessageProcessorImpl(ExceptionUtil exceptionUtil) {
    this.exceptionUtil = exceptionUtil;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

    VpSemanticErrorCodeEnum errorCode = VpSemanticErrorCodeEnum.getDefault();

    String message = throwable.getMessage();
    String messageDetails = throwable.toString();

    if (throwable instanceof VpRuntimeException) {
      VpRuntimeException exception = (VpRuntimeException) throwable;
      errorCode = exception.getErrorCode();
      messageDetails = exception.getMessageDetails();
      message = exceptionUtil.createMessage(errorCode);
    }


    SoapFaultHelper.setSoapFaultInResponse(exchange, message, messageDetails, errorCode);
    exchange.getIn().setHeader("Content-Type", "text/xml");

    log.debug("Error logged. Cause:" + message);
  }

}
