package com.g4mesoft.captureplayback.sequence;

public enum GSEChannelEntryType {

	EVENT_BOTH(0),
	EVENT_START(1),
	EVENT_END(2);

	private static final GSEChannelEntryType[] TYPES;
	
	static {
		TYPES = new GSEChannelEntryType[values().length];
		for (GSEChannelEntryType type : values())
			TYPES[type.index] = type;
	}
	
	private final int index;
	
	private GSEChannelEntryType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

	public static GSEChannelEntryType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
