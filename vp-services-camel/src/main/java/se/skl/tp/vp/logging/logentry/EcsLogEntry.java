package se.skl.tp.vp.logging.logentry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.logging.log4j.message.StringMapMessage;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.VpCodeMessages;

import java.util.*;

@Slf4j
public class EcsLogEntry extends StringMapMessage {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String EVENT_KIND_VALUE = "event";
    public static final String EVENT_MODULE_VALUE = "skltp-messages";
    public static final String EVENT_CATEGORY_VALUE;
    public static final String EVENT_TYPE_VALUE_REQ;
    public static final String EVENT_TYPE_VALUE_RESP;

    private static final EcsSystemProperties SYSTEM_PROPERTIES = EcsSystemProperties.getInstance();

    static {
        try {
            EVENT_CATEGORY_VALUE = OBJECT_MAPPER.writeValueAsString(List.of("web"));
            EVENT_TYPE_VALUE_REQ = OBJECT_MAPPER.writeValueAsString(List.of("access", "start"));
            EVENT_TYPE_VALUE_RESP = OBJECT_MAPPER.writeValueAsString(List.of("access", "end"));
        } catch (JsonProcessingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Custom label fields
    /** Parent ID for tracing, used to identify the parent operation of a span */
    public static final String LABEL_PARENT_ID = "parent.id";
    public static final String LABEL_VP_X_FORWARDED_PROTO = "httpXForwardedProto";
    public static final String LABEL_VP_X_FORWARDED_HOST = "httpXForwardedHost";
    public static final String LABEL_VP_X_FORWARDED_PORT = "httpXForwardedPort";
    public static final String LABEL_ROUTE = "route";
    public static final String LABEL_SESSION_ERROR = "sessionStatus";
    public static final String LABEL_SESSION_ERROR_DESCRIPTION = "sessionErrorDescription";
    public static final String LABEL_SESSION_ERROR_TECHNICAL_DESCRIPTION = "sessionErrorTechnicalDescription";
    public static final String LABEL_SESSION_ERROR_CODE = "errorCode";
    public static final String LABEL_SESSION_HTML_STATUS = "statusCode";
    public static final String LABEL_SENDER_ID = "senderid";
    public static final String LABEL_RECEIVER_ID = "receiverid";
    public static final String LABEL_OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaid";
    public static final String LABEL_IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaid_in";
    public static final String LABEL_ACTING_ON_BEHALF_OF_HSA_ID = "actingOnBehalfOfHsaid";
    public static final String LABEL_SERVICECONTRACT_NAMESPACE = "servicecontract_namespace";
    public static final String LABEL_RIV_VERSION = "rivversion";
    public static final String LABEL_WSDL_NAMESPACE = "wsdl_namespace";
    public static final String LABEL_VAGVAL_TRACE = "routerVagvalTrace";
    public static final String LABEL_ANROPSBEHORIGHET_TRACE = "routerBehorighetTrace";
    public static final String LABEL_SOAP_FAULT_CODE = "faultCode";
    public static final String LABEL_SOAP_FAULT_STRING = "faultString";
    public static final String LABEL_SOAP_FAULT_DETAIL = "faultDetail";
    public static final String LABEL_SSL_CONTEXT_ID = "sslContextId";

    // Fields for backward compatibility
    public static final String BACKWARD_COMPAT_LOG_MESSAGE = "LogMessage";
    public static final String BACKWARD_COMPAT_SERVICE_IMPL = "ServiceImpl";
    public static final String BACKWARD_COMPAT_COMPONENT_ID = "ComponentId";
    public static final String BACKWARD_COMPAT_ENDPOINT = "Endpoint";
    public static final String BACKWARD_COMPAT_MESSAGE_ID = "MessageId";
    public static final String BACKWARD_COMPAT_BUSINESS_CORRELATION_ID = "BusinessCorrelationId";

    // Fields for backward compatibility

    public EcsLogEntry withPayload(String payload) {
        if (payload != null) {
            String eventAction = Optional.ofNullable(get(EcsFields.EVENT_ACTION)).orElse("?");
            String key = (isReq(eventAction)) ? EcsFields.HTTP_REQUEST_BODY_CONTENT : EcsFields.HTTP_RESPONSE_BODY_CONTENT;
            put(key, payload); // Request or response body
        }
        return this;
    }

    public static class Builder {
        public static final String FILTERHEADER_REGEX = "(?i)X-Forwarded-Tls-Client-Cert|x-vp-auth-cert|x-fk-auth-cert";
        protected static final String FILTERED_TEXT = "<filtered>";
        public static final String DEFAULT_ERROR_DESCRIPTION = VpCodeMessages.getDefaultMessage();
        private final StringMapMessage data = new StringMapMessage();

        public Builder(String msgType) {
            putData(EcsFields.EVENT_ACTION, msgType);
            putData(EcsFields.EVENT_KIND, EVENT_KIND_VALUE);
            putData(EcsFields.EVENT_CATEGORY, EVENT_CATEGORY_VALUE);
            putData(EcsFields.EVENT_TYPE, isReq(msgType) ? EVENT_TYPE_VALUE_REQ : EVENT_TYPE_VALUE_RESP);
            putData(EcsFields.EVENT_MODULE, EVENT_MODULE_VALUE);
            putData(EcsFields.HOST_HOSTNAME, SYSTEM_PROPERTIES.getHostName());
            putData(EcsFields.HOST_IP, SYSTEM_PROPERTIES.getHostIp());
            putData(EcsFields.HOST_ARCHITECTURE, SYSTEM_PROPERTIES.getHostArchitecture());
            putData(EcsFields.HOST_OS_FAMILY, SYSTEM_PROPERTIES.getHostOsFamily());
            putData(EcsFields.HOST_OS_NAME, SYSTEM_PROPERTIES.getHostOsName());
            putData(EcsFields.HOST_OS_VERSION, SYSTEM_PROPERTIES.getHostOsVersion());
            putData(EcsFields.HOST_OS_PLATFORM, SYSTEM_PROPERTIES.getHostOsPlatform());
            putData(EcsFields.HOST_TYPE, SYSTEM_PROPERTIES.getHostType());
        }

        public EcsLogEntry build() {
            addBackwardCompatibilityFields();
            putData(EcsFields.MESSAGE, String.format("%s %s -> %s",
                    data.get(EcsFields.EVENT_ACTION),
                    data.get(EcsFields.LABELS + LABEL_SENDER_ID),
                    data.get(EcsFields.LABELS + LABEL_RECEIVER_ID)));
            EcsLogEntry ecsLogEntry = new EcsLogEntry();
            ecsLogEntry.putAll(data.getData());
            return ecsLogEntry;
        }

        private void addBackwardCompatibilityFields() {
            putData(BACKWARD_COMPAT_LOG_MESSAGE, data.get(EcsFields.EVENT_ACTION));
            putData(BACKWARD_COMPAT_SERVICE_IMPL, data.get(EcsFields.LABELS + LABEL_ROUTE));
            putData(BACKWARD_COMPAT_COMPONENT_ID, data.get(EcsFields.SERVICE_NAME));
            putData(BACKWARD_COMPAT_ENDPOINT, data.get(EcsFields.URL_FULL));
            putData(BACKWARD_COMPAT_MESSAGE_ID, data.get(EcsFields.TRANSACTION_ID));
            putData(BACKWARD_COMPAT_BUSINESS_CORRELATION_ID, data.get(EcsFields.TRACE_ID));
        }

        public Builder fromExchange(Exchange exchange) {
            if (exchange == null) {
                return this;
            }
            String componentId = exchange.getContext().getName();
            String serviceImplementation = exchange.getFromRouteId();
            String endpointURI = exchange.getProperty(VPExchangeProperties.HTTP_URL_IN, String.class);
            String endpoint = (endpointURI == null) ? "" : endpointURI;

            putTracing(exchange);
            putData(EcsFields.SERVICE_NAME, componentId); // Name of the service
            putLabel(LABEL_ROUTE, serviceImplementation); // Internal route identifier
            putData(EcsFields.URL_FULL, endpoint); // Full URL of the request
            putData(EcsFields.HTTP_REQUEST_METHOD, exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class));
            putData(EcsFields.HTTP_RESPONSE_STATUS_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class));

            putExtraInfo(exchange);
            return this;
        }

        private void putTracing(Exchange exchange) {
            String businessCorrelationId = exchange.getProperty(VPExchangeProperties.SKLTP_CORRELATION_ID, "", String.class);
            putData(EcsFields.TRACE_ID, businessCorrelationId);
            putData(EcsFields.TRANSACTION_ID, exchange.getExchangeId());
            putData(EcsFields.EVENT_ID, UUID.randomUUID().toString());
            String spanA = exchange.getProperty(VPExchangeProperties.SPAN_REQ_IN_TO_RESP_OUT, String.class);
            String spanB = exchange.getProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, String.class);
            if (spanB == null) {
                putData(EcsFields.SPAN_ID, spanA);
            } else {
                putLabel(LABEL_PARENT_ID, spanA);
                putData(EcsFields.SPAN_ID, spanB);
            }
        }

        public Builder withException(Exchange exchange, String stackTrace) {
            Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
            if (throwable == null) {
                return this;

            }

            putData(EcsFields.ERROR_TYPE, throwable.getClass().getName()); // The type of the error, for example the class name of the exception.
            putData(EcsFields.ERROR_MESSAGE, throwable.getMessage()); // Error message
            putData(EcsFields.ERROR_STACK_TRACE, stackTrace); // The stack trace of this error in plain text.
            return this;
        }


        public Builder withHttpForwardHeaders(Exchange exchange) {
            String httpXForwardedProto = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, String.class);
            if (httpXForwardedProto != null) {
                putLabel(LABEL_VP_X_FORWARDED_PROTO, httpXForwardedProto);
                exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO);
            }
            String httpXForwardedHost = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, String.class);
            if (httpXForwardedHost != null) {
                putLabel(LABEL_VP_X_FORWARDED_HOST, httpXForwardedHost);
                exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_HOST);
            }
            String httpXForwardedPort = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, String.class);
            if (httpXForwardedPort != null) {
                putLabel(LABEL_VP_X_FORWARDED_PORT, httpXForwardedPort);
                exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_PORT);
            }
            return this;
        }

        public void putExtraInfo(Exchange exchange) {
            String serviceContractNS = exchange.getProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, String.class);
            String rivVersion = exchange.getProperty(VPExchangeProperties.RIV_VERSION, String.class);
            Map<String, String> headers = new HashMap<>();
            filterHeaders(exchange.getIn().getHeaders(), headers);

            String senderIp = exchange.getProperty(VPExchangeProperties.SENDER_IP_ADRESS, String.class);
            putData(EcsFields.SOURCE_ADDRESS, senderIp);
            putData(EcsFields.SOURCE_IP, senderIp);
            putDestination(exchange);

            putLabel(LABEL_SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID, String.class));
            putLabel(LABEL_RECEIVER_ID, exchange.getProperty(VPExchangeProperties.RECEIVER_ID, String.class));
            putLabel(LABEL_OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID, exchange.getProperty(VPExchangeProperties.OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class));
            putLabel(LABEL_IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID, exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class));
            putLabel(LABEL_ACTING_ON_BEHALF_OF_HSA_ID, exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ACTING_ON_BEHALF_OF_HSA_ID, String.class));
            putLabel(LABEL_SERVICECONTRACT_NAMESPACE, serviceContractNS);
            putLabel(LABEL_RIV_VERSION, rivVersion);
            putLabel(LABEL_WSDL_NAMESPACE, createWsdlNamespace(serviceContractNS, rivVersion));
            putLabel(LABEL_VAGVAL_TRACE, exchange.getProperty(VPExchangeProperties.VAGVAL_TRACE, String.class));
            putLabel(LABEL_ANROPSBEHORIGHET_TRACE, exchange.getProperty(VPExchangeProperties.ANROPSBEHORIGHET_TRACE, String.class));
            putLabel(LABEL_SOAP_FAULT_CODE, exchange.getProperty(VPExchangeProperties.SOAP_FAULT_CODE, String.class));
            putLabel(LABEL_SOAP_FAULT_STRING, exchange.getProperty(VPExchangeProperties.SOAP_FAULT_STRING, String.class));
            putLabel(LABEL_SOAP_FAULT_DETAIL, exchange.getProperty(VPExchangeProperties.SOAP_FAULT_DETAIL, String.class));
            putLabel(LABEL_SSL_CONTEXT_ID, exchange.getProperty(VPExchangeProperties.SSL_CONTEXT_ID, String.class));

            String eventAction = Optional.ofNullable(data.get(EcsFields.EVENT_ACTION)).orElse("?");
            String contentLength = isReq(eventAction) ? EcsFields.HTTP_REQUEST_BODY_BYTES : EcsFields.HTTP_RESPONSE_BODY_BYTES;
            String headersKey = (isReq(eventAction)) ? EcsFields.HTTP_REQUEST_HEADERS : EcsFields.HTTP_RESPONSE_HEADERS;
            putEventDuration(exchange);
            putData(contentLength, exchange.getIn().getHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.class));
            try {
                putData(headersKey, OBJECT_MAPPER.writeValueAsString(headers));
            } catch (JsonProcessingException e) {
                putData(headersKey, "{}");
            }


            final Boolean isError = exchange.getProperty(VPExchangeProperties.SESSION_ERROR, Boolean.class);
            if (isError != null && isError) {
                putErrorInfo(exchange);
            }
        }

        private void putEventDuration(Exchange exchange) {
            if (isReq(data.get(EcsFields.EVENT_ACTION))) {
                return; // Only for response events
            }
            boolean inSpanA = exchange.getProperty(VPExchangeProperties.SPAN_REQ_OUT_TO_RESP_IN, String.class) == null;
            String responseTimeHeader = exchange.getIn().getHeader(HttpHeaders.X_SKLTP_PRODUCER_RESPONSETIME, String.class);
            if (inSpanA || responseTimeHeader == null) {
                putData(EcsFields.EVENT_DURATION, getElapsedTime(exchange).toString());
            } else {
                putData(EcsFields.EVENT_DURATION, responseTimeHeader + "000000"); // Convert milliseconds to nanoseconds
            }
        }

        private void putDestination(Exchange exchange) {
            putData(EcsFields.URL_ORIGINAL, exchange.getProperty(VPExchangeProperties.VAGVAL, String.class));
            String destination = exchange.getProperty(VPExchangeProperties.VAGVAL_HOST, String.class);
            if (destination == null) {
                return;
            }
            putData(EcsFields.DESTINATION_ADDRESS, destination);
            String[] hostAndPort = destination.split(":");
            if (hostAndPort.length > 1) {
                putData(EcsFields.DESTINATION_DOMAIN, hostAndPort[0]);
                putData(EcsFields.DESTINATION_PORT, hostAndPort[1]);
            } else {
                putData(EcsFields.DESTINATION_DOMAIN, destination);
            }
        }

        private void putErrorInfo(Exchange exchange) {
            Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            String errorMessage = exception != null ? exception.getMessage() : null;
            String errorDescription = errorMessage != null ? errorMessage : DEFAULT_ERROR_DESCRIPTION;
            String technicalDescription = exception != null ? exception.toString() : "";
            String errorCode = exchange.getProperty(VPExchangeProperties.SESSION_ERROR_CODE, String.class);
            String htmlStatus = exchange.getProperty(VPExchangeProperties.SESSION_HTML_STATUS, String.class);

            putLabel(LABEL_SESSION_ERROR, "true");
            putLabel(LABEL_SESSION_ERROR_DESCRIPTION, errorDescription);
            putLabel(LABEL_SESSION_ERROR_TECHNICAL_DESCRIPTION, technicalDescription);
            putLabel(LABEL_SESSION_ERROR_CODE, errorCode);
            putLabel(LABEL_SESSION_HTML_STATUS, htmlStatus);
        }

        @SuppressWarnings("java:S125") // URI incorrectly flagged as commented out code
        private static String createWsdlNamespace(String serviceContractNS, String profile) {
            //  Convert from interaction target namespace
            //    urn:${domänPrefix}:${tjänsteDomän}:${tjänsteInteraktion}${roll}:${m}
            //  to wsdl target namespace
            //    urn:riv:${tjänsteDomän}:${tjänsteInteraktion}:m:${profilKortnamn}
            // See https://riv-ta.atlassian.net/wiki/spaces/RTA/pages/99593635/RIV+Tekniska+Anvisningar+Tj+nsteschema
            //   and https://riv-ta.atlassian.net/wiki/spaces/RTA/pages/77856888/RIV+Tekniska+Anvisningar+Basic+Profile+2.1
            if (serviceContractNS == null || profile == null) {
                return null;
            }
            return serviceContractNS.replace("Responder", "").concat(":").concat(profile);
        }

        private static Long getElapsedTime(Exchange exchange) {
            Date created = exchange.getProperty(VPExchangeProperties.EXCHANGE_CREATED, Date.class);
            return 1000000 * (new Date().getTime() - created.getTime()); // Convert milliseconds to nanoseconds
        }

        static void filterHeaders(Map<String, Object> headers, Map<String, String> res) {
            headers.forEach((s, o) -> res.put(s, s.matches(FILTERHEADER_REGEX) ? FILTERED_TEXT : String.valueOf(o)));
        }

        private void putLabel(String source, String name) {
            putData(EcsFields.LABELS + source, name);
        }

        void putData(String key, String value) {
            if (value != null) {
                data.put(key, value);
            }
        }
    }

    private static boolean isReq(String eventAction) {
        return eventAction.startsWith("req");
    }
}
