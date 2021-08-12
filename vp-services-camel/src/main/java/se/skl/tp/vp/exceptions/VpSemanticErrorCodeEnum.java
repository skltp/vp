package se.skl.tp.vp.exceptions;

public enum VpSemanticErrorCodeEnum {
    UNSET("UNSET"),
    VP001("VP001"),
    VP002("VP002"),
    VP003("VP003"),
    VP004("VP004"),
    VP005("VP005"),
    VP006("VP006"),
    VP007("VP007"),
    VP008("VP008"),
    VP009("VP009"),
    VP010("VP010"),
    VP011("VP011"),
    VP012("VP012"),
    VP013("VP013"),
    VP014("VP014");

    private String code;
    public String getCode() {
        return code;
    }

    public VpSemanticErrorCodeEnum getCodeEnum(String code) {
        return VpSemanticErrorCodeEnum.valueOf(code);
    }

    VpSemanticErrorCodeEnum(String code) {
        this.code = code;
    }
}
