package se.skl.tp.vp.exceptions;

public enum VPFaultCodeEnum {
  Client("Client"),
  Server("Server");

  private String faultCode;

  public String getFaultCode() {
    return faultCode;
  }

  VPFaultCodeEnum(String faultCode) {
    this.faultCode = faultCode;
  }
}
