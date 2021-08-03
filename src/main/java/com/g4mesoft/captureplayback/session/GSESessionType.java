package com.g4mesoft.captureplayback.session;

public enum GSESessionType {

	COMPOSITION(0),
	SEQUENCE(1);
	
	private static final GSESessionType[] TYPES;
	
	static {
		TYPES = new GSESessionType[values().length];
		for (GSESessionType type : values())
			TYPES[type.index] = type;
	}
	
	private final int index;
	
	private GSESessionType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static GSESessionType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
