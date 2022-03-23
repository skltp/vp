package se.skl.tp.vp.errorhandling;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@Service
@Log4j2
public class HandleProducerExceptionProcessor implements Processor {

  private ExceptionUtil exceptionUtil;
  private static final String SOAP_XMLNS = "http://schemas.xmlsoap.org/soap/envelope/";
  private static final Integer HTTP_STATUS_500 = 500;

  @Autowired
  public HandleProducerExceptionProcessor(ExceptionUtil exceptionUtil) {
    this.exceptionUtil = exceptionUtil;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    try {
      Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
      if (exception != null) {
         if (exception instanceof NettyHttpOperationFailedException) {
               NettyHttpOperationFailedException operationFailedException = (NettyHttpOperationFailedException) exception;
              if (operationFailedException.getStatusCode() == HTTP_STATUS_500 && operationFailedException.getContentAsString().contains(SOAP_XMLNS)) {
                  return;
              }
          }
        String messageString = exception.getMessage();
        if (exception instanceof ReadTimeoutException) {
          messageString = "Timeout when waiting on response from producer.";
        }

        log.debug("Exception Caught by Camel when contacting producer. Exception information: " + left(messageString, 200) + "...");


        VpSemanticErrorCodeEnum errorCode = VpSemanticErrorCodeEnum.getDefault();
        String message = exceptionUtil.createMessage(errorCode);

        String addr = exchange.getProperty(VPExchangeProperties.VAGVAL, "<UNKNOWN>", String.class);
        String vpMsg = String.format("%s. Exception Caught by Camel when contacting producer. Exception information: (%s: %s)",
            addr, exception.getClass().getName(), messageString);
        String messageDetails = exceptionUtil.createDetailsMessage(errorCode, vpMsg);

        SoapFaultHelper.setSoapFaultInResponse(exchange, message, messageDetails, VpSemanticErrorCodeEnum.getDefault());
      }
    } catch (Exception e) {
      log.error("An error occured in HandleProducerExceptionProcessor", e);
      throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.getDefault(), "unknown");
    }

  }

  private String left(String s, int len) {
    if (s == null) {
      return null;
    }

    int i = s.length() > len ? len : s.length();
    return s.substring(0, i);
  }


}
