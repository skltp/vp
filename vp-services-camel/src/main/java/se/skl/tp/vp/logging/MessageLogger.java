package se.skl.tp.vp.logging;

import org.apache.camel.Exchange;

/**
 * Interface for message logging implementations.
 * Implementations include ECS format and Legacy format loggers.
 */
public interface MessageLogger {

    // Logger names used by all implementations
    String REQ_IN = "se.skl.tp.vp.logging.req.in";
    String REQ_OUT = "se.skl.tp.vp.logging.req.out";
    String RESP_IN = "se.skl.tp.vp.logging.resp.in";
    String RESP_OUT = "se.skl.tp.vp.logging.resp.out";
    String REQ_ERROR = "se.skl.tp.vp.logging.error";

    /**
     * Log incoming request message.
     * @param exchange the Camel exchange
     */
    void logReqIn(Exchange exchange);

    /**
     * Log outgoing request message to producer.
     * @param exchange the Camel exchange
     */
    void logReqOut(Exchange exchange);

    /**
     * Log incoming response message from producer.
     * @param exchange the Camel exchange
     */
    void logRespIn(Exchange exchange);

    /**
     * Log outgoing response message to consumer.
     * @param exchange the Camel exchange
     */
    void logRespOut(Exchange exchange);

    /**
     * Log error message.
     * @param exchange the Camel exchange
     * @param stackTrace the stack trace string
     */
    void logError(Exchange exchange, String stackTrace);
}

