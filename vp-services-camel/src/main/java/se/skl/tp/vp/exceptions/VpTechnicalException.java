package se.skl.tp.vp.exceptions;

public class VpTechnicalException extends VpRuntimeException {

	private static final long serialVersionUID = 1L;

	public VpTechnicalException(VpSemanticErrorCodeEnum errorCode, String message, String messageDetails) {
		super(errorCode, message, messageDetails);
	}

	public VpTechnicalException(VpSemanticErrorCodeEnum errorCode, String message, String messageDetails, Throwable cause) {
		super(errorCode, message, messageDetails, cause);
	}
}
