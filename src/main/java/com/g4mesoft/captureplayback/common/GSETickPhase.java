package com.g4mesoft.captureplayback.common;

public enum GSETickPhase {

	BLOCK_EVENTS(0);

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
