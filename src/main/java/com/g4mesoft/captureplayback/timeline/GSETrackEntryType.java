package com.g4mesoft.captureplayback.timeline;

public enum GSETrackEntryType {

	EVENT_BOTH(0),
	EVENT_START(1),
	EVENT_END(2);

	private static final GSETrackEntryType[] TYPES;
	
	static {
		TYPES = new GSETrackEntryType[values().length];
		for (GSETrackEntryType type : values())
			TYPES[type.index] = type;
	}
	
	private final int index;
	
	private GSETrackEntryType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

	public static GSETrackEntryType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
