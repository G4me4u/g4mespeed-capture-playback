package com.g4mesoft.captureplayback.common.asset;

public enum GSEAssetType {

	COMPOSITION("composition", true, 0),
	SEQUENCE("sequence", true, 1);
	
	private static final GSEAssetType[] TYPES;
	
	static {
		TYPES = new GSEAssetType[values().length];
		for (GSEAssetType type : values())
			TYPES[type.index] = type;
	}
	
	private final String name;
	private final boolean streamable;
	private final int index;
	
	private GSEAssetType(String name, boolean streamable, int index) {
		this.name = name;
		this.streamable = streamable;
		this.index = index;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isStreamable() {
		return streamable;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static GSEAssetType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
