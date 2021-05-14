package com.g4mesoft.captureplayback.common;

public enum GSETickPhase {

	IMMEDIATE(0),
	BLOCK_EVENTS(1);

	private final int order;
	
	private GSETickPhase(int order) {
		this.order = order;
	}
	
	public boolean isBefore(GSETickPhase other) {
		return (order < other.order);
	}

	public boolean isAfter(GSETickPhase other) {
		return (order > other.order);
	}
}
