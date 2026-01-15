package se.skl.tp.vp.errorhandling;

/**
 * Record containing extracted SOAP fault information.
 *
 * @param faultCode The SOAP fault code (e.g., "soap:Server")
 * @param faultString The SOAP fault string describing the error
 * @param detail The detailed fault information as serialized XML (may contain application-specific structured data)
 */
public record SoapFaultInfo(
        String faultCode,
        String faultString,
        String detail
) {

  /**
   * @return true if any fault information was extracted
   */
  public boolean hasFaultInfo() {
    return faultCode != null || faultString != null || detail != null;
  }
}

