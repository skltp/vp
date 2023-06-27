package se.skl.tp.vp.exceptions;

import static se.skl.tp.vp.exceptions.VPFaultCodeEnum.Client;
import static se.skl.tp.vp.exceptions.VPFaultCodeEnum.Server;

public enum VpSemanticErrorCodeEnum {
    UNSET("UNSET", Server),
    VP001("VP001", Client),
    VP002("VP002", Client),
    VP003("VP003", Client),
    VP004("VP004", Client),
    VP005("VP005", Client),
    VP006("VP006", Server),
    VP007("VP007", Server),
    VP008("VP008", Server),
    VP009("VP009", Server),
    VP010("VP010", Server),
    VP011("VP011", Client),
    VP012("VP012", Server),
    VP013("VP013", Client),
    VP014("VP014", Server),
    VP015("VP015", Client);


    private String vpDigitErrorCode;
    private VPFaultCodeEnum faultCode;

    public String getVpDigitErrorCode() {
        return vpDigitErrorCode;
    }

    public static VpSemanticErrorCodeEnum getDefault(){
        return VP009;
    }


    public String getFaultCode() { return faultCode.getFaultCode();
    }

    public VpSemanticErrorCodeEnum getCodeEnum(String code) {
        return VpSemanticErrorCodeEnum.valueOf(code);
    }

    VpSemanticErrorCodeEnum(String vpDigitErrorCode, VPFaultCodeEnum faultCode) {
        this.faultCode = faultCode;
        this.vpDigitErrorCode = vpDigitErrorCode;
    }
}
