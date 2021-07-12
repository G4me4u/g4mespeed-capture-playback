package com.g4mesoft.captureplayback.sequence.delta;

public class GSSequenceDeltaException extends Exception {
	private static final long serialVersionUID = -4289873719094461632L;

	public GSSequenceDeltaException() {
	}
	
	public GSSequenceDeltaException(String msg) {
		super(msg);
	}

	public GSSequenceDeltaException(Throwable cause) {
		super(cause);
	}

	public GSSequenceDeltaException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
