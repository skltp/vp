package se.skl.tp.vp.exceptions;

public class VpTechnicalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VpTechnicalException() {
	}

	public VpTechnicalException(String message) {
		super(message);
	}

	public VpTechnicalException(Throwable cause) {
		super(cause);
	}

	public VpTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

}
