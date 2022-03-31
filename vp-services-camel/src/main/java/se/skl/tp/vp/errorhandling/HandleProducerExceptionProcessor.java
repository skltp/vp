package se.skl.tp.vp.errorhandling;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@Service
@Log4j2
/**
 * This function processes custom handling of Exceptions raised by data Producers.
 */
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

                // placeholder for tracking HTTP Status Code.
                Integer statusCode = null;

                // Detect Netty HTTP Operational Failure. Record HTTP Status Code.
                //   Abort if HTTP 500 AND SOAP-schema reference present in exception.
                if (exception instanceof NettyHttpOperationFailedException) {
                    NettyHttpOperationFailedException opFailedException = (NettyHttpOperationFailedException) exception;
                    statusCode = opFailedException.getStatusCode();

                    if (statusCode.equals(HTTP_STATUS_500)
                            && opFailedException.getContentAsString().contains(SOAP_XMLNS)) {
                        return;
                    }
                }

                // Detect general HTTP Operational Failure. Record HTTP Status Code.
                if (exception instanceof HttpOperationFailedException) {
                    HttpOperationFailedException opFailedException = (HttpOperationFailedException) exception;
                    statusCode = opFailedException.getStatusCode();
                }


                // Record Exception message.
                String messageString = exception.getMessage();

                // Override above Exception message if it was due to a timeout.
                if (exception instanceof ReadTimeoutException) {
                    messageString = "Timeout when waiting on response from producer.";
                }

                log.debug("Exception Caught by Camel when contacting producer. Exception information: "
                        + truncateToMaxLength(messageString, 200) + "...");

                // Prepare response in accordance to VP problem code standards.
                VpSemanticErrorCodeEnum errorCode = VpSemanticErrorCodeEnum.getDefault();
                String message = exceptionUtil.createMessage(errorCode);

                String address = (String) exchange.getProperty(VPExchangeProperties.VAGVAL, "<UNKNOWN>");
                String vpMessage = String.format(
                        "%s. Exception Caught by Camel when contacting producer. Exception information: (%s: %s)",
                        address,
                        exception.getClass().getName(),
                        messageString
                );

                // If a status code was recorded in prior steps, append it to the message component that will form
                //   part of the faultDetails element.
                if (statusCode != null) {
                    vpMessage = vpMessage.concat(
                            String.format(
                                    "\nHTTP Exception occurred during communication. Classification: VP_PRODUCER_HTTP_STATUS=%s",
                                    statusCode)
                    );
                }

                // Assemble a SOAP Fault response.
                String messageDetails = exceptionUtil.createDetailsMessage(errorCode, vpMessage);
                SoapFaultHelper.setSoapFaultInResponse(exchange, message, messageDetails, VpSemanticErrorCodeEnum.getDefault());
            }
        } catch (Exception e) {
            log.error("An error occured in HandleProducerExceptionProcessor", e);
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.getDefault(), "unknown");
        }
    }

    /**
     * This function will take a text, and truncate it to provided length value if it is too long.
     * @param text Text to be truncated, if too long.
     * @param length Max length of text.
     * @return A potentially truncated string, or the entire string if short enough.
     */
    private String truncateToMaxLength(String text, int length) {
        if (text == null) {
            return null;
        }

        int i = Math.min(text.length(), length);
        return text.substring(0, i);
    }
}
