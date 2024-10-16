package se.skl.tp.vp;

import static org.apache.camel.builder.PredicateBuilder.or;

import io.netty.handler.timeout.ReadTimeoutException;
import java.net.SocketException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.certificate.CertificateExtractorProcessor;
import se.skl.tp.vp.charset.ConvertRequestCharset;
import se.skl.tp.vp.charset.ConvertResponseCharset;
import se.skl.tp.vp.config.HttpHeaderFilterProperties;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionMessageProcessor;
import se.skl.tp.vp.errorhandling.HandleEmptyResponseProcessor;
import se.skl.tp.vp.errorhandling.HandleProducerExceptionProcessor;
import se.skl.tp.vp.httpheader.HttpSenderIdExtractorProcessor;
import se.skl.tp.vp.httpheader.OriginalConsumerIdProcessor;
import se.skl.tp.vp.httpheader.OutHeaderProcessor;
import se.skl.tp.vp.logging.MessageInfoLogger;
import se.skl.tp.vp.requestreader.RequestReaderProcessor;
import se.skl.tp.vp.timeout.RequestTimoutProcessor;
import se.skl.tp.vp.vagval.BehorighetProcessor;
import se.skl.tp.vp.vagval.RivTaProfilProcessor;
import se.skl.tp.vp.vagval.VagvalProcessor;
import se.skl.tp.vp.wsdl.WsdlProcessor;

@Component
public class VPRouter extends RouteBuilder {

    public static final String VP_HTTP_ROUTE = "vp-http-route";
    public static final String VP_HTTPS_ROUTE = "vp-https-route";
    public static final String VAGVAL_ROUTE = "vagval-route";
    public static final String TO_PRODUCER_ROUTE = "to-producer-route";
    public static final String DIRECT_VP = "direct:vp";
    public static final String DIRECT_PRODUCER_ROUTE = "direct:to-producer";
    public static final String DIRECT_PRODUCER_ERROR = "direct:producer-error";
    public static final String DIRECT_WSDL = "direct:wsdl";

    public static final String NETTY_HTTPS_INCOMING_FROM = "netty-http:{{vp.https.route.url}}?"
        + "sslContextParameters=#incomingSSLContextParameters&ssl=true&"
        + "sslClientCertHeaders=true&"
        + "needClientAuth=true&"
        + "matchOnUriPrefix=true&"
        + "chunkedMaxContentLength={{vp.max.receive.length}}&"
        + "nettyHttpBinding=#VPNettyHttpBinding";
    public static final String NETTY_HTTP_FROM = "netty-http:{{vp.http.route.url}}?"
        + "matchOnUriPrefix=true&"
        + "chunkedMaxContentLength={{vp.max.receive.length}}&"
        + "nettyHttpBinding=#VPNettyHttpBinding";
    public static final String NETTY_HTTP_OUTGOING_TOD = "netty-http:http://${exchangeProperty.vagvalHost}?"
        + "useRelativePath=true&"
        + "nettyHttpBinding=#VPNettyHttpBinding&"
        + "chunkedMaxContentLength={{vp.max.receive.length}}&"
        + "disconnect={{producer.http.disconnect}}&"
        + "keepAlive={{producer.http.keepAlive}}&"
        + "workerGroup=#sharedClientHttpPool&"
        + "connectTimeout={{producer.http.connect.timeout}}";
    public static final String NETTY_HTTPS_OUTGOING_TOD = "netty-http:https://${exchangeProperty.vagvalHost}?"
        + "sslContextParameters=#outgoingSSLContextParameters&"
        + "ssl=true&"
        + "hostnameVerification={{producer.https.hostnameVerification}}&"
        + "useRelativePath=true&"
        + "nettyHttpBinding=#VPNettyHttpBinding&"
        + "chunkedMaxContentLength={{vp.max.receive.length}}&"
        + "disconnect={{producer.https.disconnect}}&"
        + "keepAlive={{producer.https.keepAlive}}&"
        + "workerGroup=#sharedClientHttpsPool&"
        + "connectTimeout={{producer.https.connect.timeout}}";

    public static final String VAGVAL_PROCESSOR_ID = "VagvalProcessor";
    public static final String BEHORIGHET_PROCESSOR_ID = "BehorighetProcessor";
    public static final String LOG_ERROR_METHOD = "logError(*,${exception.stacktrace})";
    public static final String LOG_EMPTY_METHOD = "logError(*,${null})";
    public static final String LOG_RESP_OUT_METHOD = "logRespOut(*)";
    public static final String LOG_REQ_IN_METHOD = "logReqIn(*)";
    public static final String LOG_REQ_OUT_METHOD = "logReqOut(*)";
    public static final String LOG_RESP_IN_METHOD = "logRespIn(*)";

    @Autowired
    OriginalConsumerIdProcessor originalConsumerIdProcessor;

    @Autowired
    OutHeaderProcessor setOutHeadersProcessor;

    @Autowired
    VagvalProcessor vagvalProcessor;

    @Autowired
    BehorighetProcessor behorighetProcessor;

    @Autowired
    CertificateExtractorProcessor certificateExtractorProcessor;

    @Autowired
    HttpSenderIdExtractorProcessor httpSenderIdExtractorProcessor;

    @Autowired
    RequestReaderProcessor requestReaderProcessor;

    @Autowired
    ExceptionMessageProcessor exceptionMessageProcessor;

    @Autowired
    HandleEmptyResponseProcessor handleEmptyResponseProcessor;

    @Autowired
    RivTaProfilProcessor rivTaProfilProcessor;

    @Autowired
    WsdlProcessor wsdlProcessor;

    @Autowired
    RequestTimoutProcessor requestTimoutProcessor;

    @Autowired
    HandleProducerExceptionProcessor handleProducerExceptionProcessor;

    @Autowired
    private HttpHeaderFilterProperties headerFilter;

    @Autowired
    private ConvertRequestCharset convertRequestCharset;

    @Autowired
    private ConvertResponseCharset convertResponseCharset;

    @Override
    @SuppressWarnings("unchecked") // Caused by Camel's onException method
    public void configure() throws Exception {

        onException(Exception.class)
            .process(exceptionMessageProcessor)
            .bean(MessageInfoLogger.class, LOG_ERROR_METHOD)
            .process(convertResponseCharset)
            .removeHeaders(headerFilter.getResponseHeadersToRemove(), headerFilter.getResponseHeadersToKeep())
            .bean(MessageInfoLogger.class, LOG_RESP_OUT_METHOD)
            .handled(true);


        from(NETTY_HTTPS_INCOMING_FROM).routeId(VP_HTTPS_ROUTE)
        	.setProperty(VPExchangeProperties.EXCHANGE_CREATED,  simple("${date:exchangeCreated}"))
            .choice()
              .when(header("wsdl").isNotNull()).to(DIRECT_WSDL)
              .when(header("xsd").isNotNull()).to(DIRECT_WSDL)
            .otherwise()
                .process(certificateExtractorProcessor)
                .to(DIRECT_VP)
                .removeHeaders(headerFilter.getResponseHeadersToRemove(), headerFilter.getResponseHeadersToKeep())
                .bean(MessageInfoLogger.class, LOG_RESP_OUT_METHOD)
            .end();

        from(NETTY_HTTP_FROM).routeId(VP_HTTP_ROUTE)
        	.setProperty(VPExchangeProperties.EXCHANGE_CREATED,  simple("${date:exchangeCreated}"))
            .choice()
              .when(header("wsdl").isNotNull()).to(DIRECT_WSDL)
              .when(header("xsd").isNotNull()).to(DIRECT_WSDL)
            .otherwise()
                .process(httpSenderIdExtractorProcessor)
                .to(DIRECT_VP)
                .removeHeaders(headerFilter.getResponseHeadersToRemove(), headerFilter.getResponseHeadersToKeep())
                .bean(MessageInfoLogger.class, LOG_RESP_OUT_METHOD)
            .end();

        from(DIRECT_VP).routeId(VAGVAL_ROUTE)
            .streamCaching()
            .setProperty(VPExchangeProperties.HTTP_URL_IN,  header(Exchange.HTTP_URL))
            .setProperty(VPExchangeProperties.VP_X_FORWARDED_HOST,  header("{{http.forwarded.header.host}}"))
            .setProperty(VPExchangeProperties.VP_X_FORWARDED_PORT,  header("{{http.forwarded.header.port}}"))
            .setProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO,  header("{{http.forwarded.header.proto}}"))
            .process(requestReaderProcessor)
            .process(originalConsumerIdProcessor)
            .bean(MessageInfoLogger.class, LOG_REQ_IN_METHOD)
            .process(vagvalProcessor).id(VAGVAL_PROCESSOR_ID)
            .process(behorighetProcessor).id(BEHORIGHET_PROCESSOR_ID)
            .process(requestTimoutProcessor)
            .process(rivTaProfilProcessor)
            .process(setOutHeadersProcessor)
            .to(DIRECT_PRODUCER_ROUTE)
            .choice().when(or(body().isNull(), body().isEqualTo("")))
                .log(LoggingLevel.WARN, "Response from producer is empty")
                .process(handleEmptyResponseProcessor)
                .bean(MessageInfoLogger.class, LOG_EMPTY_METHOD)
            .end();

        from(DIRECT_PRODUCER_ROUTE)
            .routeId(TO_PRODUCER_ROUTE)

            .onException(SocketException.class)
                .asyncDelayedRedelivery()
                .redeliveryDelay("{{vp.producer.retry.delay}}")
                .maximumRedeliveries("{{vp.producer.retry.attempts}}")
                    .logRetryAttempted(true)
                    .retryAttemptedLogLevel(LoggingLevel.WARN)
                    .logRetryStackTrace(false)
                .to(DIRECT_PRODUCER_ERROR)
                .handled(true)
            .end()
            .onException(ReadTimeoutException.class, NettyHttpOperationFailedException.class)
                .to(DIRECT_PRODUCER_ERROR)
                .handled(true)
            .end()

            .process(convertRequestCharset)
            .removeHeaders(headerFilter.getRequestHeadersToRemove(), headerFilter.getRequestHeadersToKeep())
            .bean(MessageInfoLogger.class, LOG_REQ_OUT_METHOD)
            .choice().when(exchangeProperty(VPExchangeProperties.VAGVAL).contains("https://"))
                    .toD(NETTY_HTTPS_OUTGOING_TOD)
                    .endChoice()
                .otherwise()
                    .toD(NETTY_HTTP_OUTGOING_TOD)
                    .endChoice()
            .end()
            .bean(MessageInfoLogger.class, LOG_RESP_IN_METHOD)
            .process(convertResponseCharset)
            .end();

        from(DIRECT_PRODUCER_ERROR)
            .process(handleProducerExceptionProcessor)
            .choice()
	            .when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo("200"))
	                .bean(MessageInfoLogger.class, LOG_ERROR_METHOD)
	            .end()
            .end()
            .process(convertResponseCharset)
            .removeHeaders(headerFilter.getResponseHeadersToRemove(), headerFilter.getResponseHeadersToKeep())
            .bean(MessageInfoLogger.class, LOG_RESP_OUT_METHOD)
            // Always return status 500 to when soap fault
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500));

        from(DIRECT_WSDL)
            .process(wsdlProcessor)
            .removeHeaders(headerFilter.getRequestHeadersToRemove(), headerFilter.getRequestHeadersToKeep())
            .removeHeaders(headerFilter.getResponseHeadersToRemove(), headerFilter.getResponseHeadersToKeep())
        .end();

    }
}
