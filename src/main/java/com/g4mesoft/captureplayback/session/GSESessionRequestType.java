package com.g4mesoft.captureplayback.session;

public enum GSESessionRequestType {

	REQUEST_START(0),
	REQUEST_STOP(1);
	
	private static final GSESessionRequestType[] TYPES;
	
	static {
		TYPES = new GSESessionRequestType[values().length];
		for (GSESessionRequestType type : values())
			TYPES[type.index] = type;
	}
	
	private final int index;
	
	private GSESessionRequestType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static GSESessionRequestType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
