package se.skl.tp.vp.exceptions;

public class VpSemanticException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private VpSemanticErrorCodeEnum errorCode;

	public VpSemanticException(String message, VpSemanticErrorCodeEnum errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public VpSemanticException(String message,
                               VpSemanticErrorCodeEnum errorCode, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public VpSemanticErrorCodeEnum getErrorCode() {
		// never return null, eliminate need for null-checking
		if (errorCode != null) {
			return errorCode;
		}
		return VpSemanticErrorCodeEnum.UNSET;
	}

}
