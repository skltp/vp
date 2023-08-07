package se.skl.tp.vp.exceptions;

public abstract class VpRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private VpSemanticErrorCodeEnum errorCode;
    private String messageDetails;

    public VpRuntimeException(VpSemanticErrorCodeEnum errorCode, String message,
                               String messageDetails) {
        super(message);
        this.messageDetails = messageDetails;
        this.errorCode = errorCode;
    }

    public VpRuntimeException(VpSemanticErrorCodeEnum errorCode, String message,
                               String messageDetails,
                               Throwable cause) {
        super(message, cause);
        this.messageDetails = messageDetails;
        this.errorCode = errorCode;
    }

    public String getMessageDetails(){
        return messageDetails;
    }

    public VpSemanticErrorCodeEnum getErrorCode() {
        // never return null, eliminate need for null-checking
        if (errorCode != null) {
            return errorCode;
        }
        return VpSemanticErrorCodeEnum.UNSET;
    }

}
