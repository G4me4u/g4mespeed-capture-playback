package com.g4mesoft.captureplayback.composition.delta;

public class GSCompositionDeltaException extends Exception {
	private static final long serialVersionUID = -8457481380010414434L;

	public GSCompositionDeltaException() {
	}
	
	public GSCompositionDeltaException(String msg) {
		super(msg);
	}

	public GSCompositionDeltaException(Throwable cause) {
		super(cause);
	}

	public GSCompositionDeltaException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
