package se.skl.tp.vp.errorhandling;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.exceptions.VPFaultCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

/**
 * Builds the SOAP Fault returned when an incoming consumer request exceeds
 * {@code vp.max.receive.length}.
 *
 * <p>That situation is detected and handled entirely inside Netty's
 * {@link io.netty.handler.codec.http.HttpObjectAggregator}, before any Camel exchange is created,
 * so the normal Camel based error handling cannot be used. This component pre-builds the VP015 SOAP
 * Fault once at startup and exposes it through static accessors that the (classpath shadowed)
 * aggregator can call without access to the Spring context.
 */
@Component
@Log4j2
public class OversizedRequestFault {

  private static final AtomicReference<byte[]> soapFaultBody = new AtomicReference<>(fallbackFault());

  private final ExceptionUtil exceptionUtil;

  @Autowired
  public OversizedRequestFault(ExceptionUtil exceptionUtil) {
    this.exceptionUtil = exceptionUtil;
  }

  @PostConstruct
  void init() {
    String faultString = exceptionUtil.createMessage(VpSemanticErrorCodeEnum.VP015);
    String xml = SoapFaultHelper.generateSoap11FaultWithCause(faultString, VPFaultCodeEnum.Client);
    soapFaultBody.set(xml.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @return the VP015 SOAP Fault body (UTF-8). Falls back to a minimal fault if the Spring context
   *     was not initialized yet.
   */
  public static byte[] soapFaultBody() {
    return soapFaultBody.get().clone();
  }

  /**
   * Logs a single error line for a rejected, oversized consumer request. Full ECS message logging is
   * not available here since the request never enters Camel.
   */
  public static void logOversizedRequest(String remoteAddress) {
    log.error("Rejected oversized incoming request from {}. errorCode={} ({})",
        remoteAddress, VpSemanticErrorCodeEnum.VP015, "request exceeded vp.max.receive.length");
  }

  private static byte[] fallbackFault() {
    String faultString = VpSemanticErrorCodeEnum.VP015
        + " Anrop ej korrekt utformat och kan därför inte behandlas.";
    return SoapFaultHelper.generateSoap11FaultWithCause(faultString, VPFaultCodeEnum.Client)
        .getBytes(StandardCharsets.UTF_8);
  }
}
