package se.skl.tp.vp.logging.old;


import io.netty.handler.codec.http.HttpHeaderNames;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.camel.Exchange;

import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.VpCodeMessages;

public class LogExtraInfoBuilder {

  public static final String SENDER_ID = VPExchangeProperties.SENDER_ID;
  public static final String RECEIVER_ID = VPExchangeProperties.RECEIVER_ID;
  public static final String RIV_VERSION = VPExchangeProperties.RIV_VERSION;
  public static final String SERVICECONTRACT_NAMESPACE = VPExchangeProperties.SERVICECONTRACT_NAMESPACE;
  public static final String WSDL_NAMESPACE = "wsdl_namespace";
  public static final String SENDER_IP_ADRESS = VPExchangeProperties.SENDER_IP_ADRESS;
  public static final String VAGVAL_TRACE = VPExchangeProperties.VAGVAL_TRACE;
  public static final String ANROPSBEHORIGHET_TRACE = VPExchangeProperties.ANROPSBEHORIGHET_TRACE;
  public static final String OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaid";
  public static final String IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "originalServiceconsumerHsaid_in";
  public static final String ACTING_ON_BEHALF_OF_HSA_ID = "actingOnBehalfOfHsaid";
  public static final String TIME_ELAPSED = "time.elapsed";
  public static final String TIME_PRODUCER = "time.producer";
  public static final String MESSAGE_LENGTH = "message.length";
  public static final String HEADERS = "Headers";
  public static final String ENDPOINT_URL = "endpoint_url";
  public static final String SOURCE = "source";
  public static final String VP_X_FORWARDED_HOST = VPExchangeProperties.VP_X_FORWARDED_HOST;
  public static final String VP_X_FORWARDED_PROTO = VPExchangeProperties.VP_X_FORWARDED_PROTO;
  public static final String VP_X_FORWARDED_PORT = VPExchangeProperties.VP_X_FORWARDED_PORT;
  public static final String DEFAULT_ERROR_DESCRIPTION = VpCodeMessages.getDefaultMessage();
  // Case insensitive regex to match what headers to filter out. This should of course be loaded through conf, but alas, we have static fields.
  // This class may be a good candidate for refactoring!
  public static final String FILTERHEADER_REGEX = "(?i)X-Forwarded-Tls-Client-Cert|x-vp-auth-cert|x-fk-auth-cert";
  
  protected static final String FILTERED_TEXT = "<filtered>";

  private LogExtraInfoBuilder() {
    // Static utility class
  }

  public static Map<String, String> createExtraInfo(Exchange exchange) {
    ExtraInfoMap<String, String> extraInfo = new ExtraInfoMap<>();

    extraInfo.put(SENDER_ID, exchange.getProperty(VPExchangeProperties.SENDER_ID, String.class));
    extraInfo.put(RECEIVER_ID, exchange.getProperty(VPExchangeProperties.RECEIVER_ID, String.class));
    extraInfo.put(OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID,
        exchange.getProperty(VPExchangeProperties.OUT_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class));
    extraInfo.putNotEmpty(IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID,
        nullValue2Blank(exchange.getProperty(VPExchangeProperties.IN_ORIGINAL_SERVICE_CONSUMER_HSA_ID, String.class)));
    extraInfo.put(SENDER_IP_ADRESS, exchange.getProperty(VPExchangeProperties.SENDER_IP_ADRESS, String.class));
    extraInfo.putNotNull(ACTING_ON_BEHALF_OF_HSA_ID,
        exchange.getIn().getHeader(HttpHeaders.X_RIVTA_ACTING_ON_BEHALF_OF_HSA_ID, String.class));

    String serviceContractNS = exchange.getProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, String.class);
    String rivVersion = exchange.getProperty(VPExchangeProperties.RIV_VERSION, String.class);
    extraInfo.put(SERVICECONTRACT_NAMESPACE, serviceContractNS);
    extraInfo.put(RIV_VERSION, rivVersion);
    extraInfo.put(WSDL_NAMESPACE, createWsdlNamespace(serviceContractNS, rivVersion));

    Map<String, String> headers = new HashMap<>();
    filterHeaders(exchange.getIn().getHeaders(), FILTERHEADER_REGEX, headers);
    extraInfo.put(HEADERS, getHeadersAsString(headers));
    extraInfo.put(TIME_ELAPSED, getElapsedTime(exchange).toString());

    extraInfo.putNotNull(VAGVAL_TRACE, exchange.getProperty(VPExchangeProperties.VAGVAL_TRACE, String.class));
    extraInfo.putNotNull(ANROPSBEHORIGHET_TRACE, exchange.getProperty(VPExchangeProperties.ANROPSBEHORIGHET_TRACE, String.class));
    extraInfo.putNotNull(ENDPOINT_URL, exchange.getProperty(VPExchangeProperties.VAGVAL, String.class));

    String timeProducer = exchange.getIn().getHeader(HttpHeaders.X_SKLTP_PRODUCER_RESPONSETIME, String.class);
    extraInfo.putNotNull(TIME_PRODUCER, timeProducer);

    String messageSize = exchange.getIn().getHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.class);
    extraInfo.putNotNull(MESSAGE_LENGTH, messageSize);

    addHttpForwardHeaders(exchange, extraInfo);

    final Boolean isError = exchange.getProperty(VPExchangeProperties.SESSION_ERROR, Boolean.class);
    if (isError != null && isError) {
      addErrorInfo(exchange, extraInfo);
    }
    return extraInfo;
  }

  private static void addHttpForwardHeaders(Exchange exchange, ExtraInfoMap<String, String> extraInfo) {
    // Following should be logged only once

    String httpXForwardedProto = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO, String.class);
    if (httpXForwardedProto != null) {
      extraInfo.put(VP_X_FORWARDED_PROTO, httpXForwardedProto);
      exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_PROTO);
    }
    String httpXForwardedHost = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_HOST, String.class);
    if (httpXForwardedHost != null) {
      extraInfo.put(VP_X_FORWARDED_HOST, httpXForwardedHost);
      exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_HOST);
    }
    String httpXForwardedPort = exchange.getProperty(VPExchangeProperties.VP_X_FORWARDED_PORT, String.class);
    if (httpXForwardedPort != null) {
      extraInfo.put(VP_X_FORWARDED_PORT, httpXForwardedPort);
      exchange.removeProperty(VPExchangeProperties.VP_X_FORWARDED_PORT);
    }
  }

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
    return new Date().getTime() - created.getTime();
  }

  private static void addErrorInfo(Exchange exchange, ExtraInfoMap<String, String> extraInfo) {
    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
    String errorMessage = exception!=null ? exception.getMessage() : null;
    String errorDescription = errorMessage!=null ? errorMessage : DEFAULT_ERROR_DESCRIPTION;
    String technicalDescription = exception!=null ? exception.toString() : "";
    String errorCode = exchange.getProperty(VPExchangeProperties.SESSION_ERROR_CODE, String.class);
    String htmlStatus = exchange.getProperty(VPExchangeProperties.SESSION_HTML_STATUS, String.class);

    extraInfo.put(VPExchangeProperties.SESSION_ERROR, "true");
    extraInfo.put(VPExchangeProperties.SESSION_ERROR_DESCRIPTION, nullValue2Blank(errorDescription));
    extraInfo.put(VPExchangeProperties.SESSION_ERROR_TECHNICAL_DESCRIPTION, nullValue2Blank(technicalDescription));
    extraInfo.put(VPExchangeProperties.SESSION_ERROR_CODE, nullValue2Blank(errorCode));
    extraInfo.putNotEmpty(VPExchangeProperties.SESSION_HTML_STATUS, htmlStatus);

  }

  private static String nullValue2Blank(String s) {
    return (s == null) ? "" : s;
  }

  static void filterHeaders(Map<String, Object> headers, String filterRegex, Map<String, String> res) {
    headers.forEach((s, o) -> res.put(s, s.matches(filterRegex) ? FILTERED_TEXT : String.valueOf(o)));
  }

  private static String getHeadersAsString(Map<String, ?> headersMap) {
    return headersMap.keySet().stream().map(key -> key + "=" + headersMap.get(key))
            .collect(Collectors.joining(", ", "{", "}"));
  }

  private static class ExtraInfoMap<K, V> extends HashMap<K, V> {

    public V putNotNull(K key, V value) {
      return value == null ? null : put(key, value);
    }

    public V putNotEmpty(K key, V value) {
      return (value == null || ((String) value).isEmpty()) ? null : put(key, value);
    }
  }
}
