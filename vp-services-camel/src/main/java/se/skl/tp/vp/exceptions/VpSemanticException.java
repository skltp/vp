package se.skl.tp.vp.exceptions;

public class VpSemanticException extends VpRuntimeException {

	public VpSemanticException(VpSemanticErrorCodeEnum errorCode, String message, String messageDetails) {
		super(errorCode, message, messageDetails);
	}

	public VpSemanticException(VpSemanticErrorCodeEnum errorCode, String message, String messageDetails, Throwable cause) {
		super(errorCode, message, messageDetails, cause);
	}
}
