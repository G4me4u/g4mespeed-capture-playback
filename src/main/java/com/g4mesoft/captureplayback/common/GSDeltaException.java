package com.g4mesoft.captureplayback.common;

public class GSDeltaException extends Exception {
	private static final long serialVersionUID = -8457481380010414434L;

	public GSDeltaException() {
	}
	
	public GSDeltaException(String msg) {
		super(msg);
	}

	public GSDeltaException(Throwable cause) {
		super(cause);
	}

	public GSDeltaException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
