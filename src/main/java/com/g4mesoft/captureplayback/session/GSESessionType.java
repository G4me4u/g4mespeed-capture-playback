package com.g4mesoft.captureplayback.session;

public enum GSESessionType {

	COMPOSITION("composition", 0),
	SEQUENCE("sequence", 1),
	PLAYLIST("playlist", 2);
	
	private static final GSESessionType[] TYPES;
	
	static {
		TYPES = new GSESessionType[values().length];
		for (GSESessionType type : values())
			TYPES[type.index] = type;
	}
	
	private final String name;
	private final int index;
	
	private GSESessionType(String name, int index) {
		this.name = name;
		this.index = index;
	}
	
	public String getName() {
		return name;
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
