package com.g4mesoft.captureplayback.sequence;

public enum GSEChannelEntryType {

	EVENT_BOTH(0, "panel.sequencecontent.typeboth"),
	EVENT_START(1, "panel.sequencecontent.typestart"),
	EVENT_END(2, "panel.sequencecontent.typeend");

	public static final GSEChannelEntryType[] TYPES;
	
	static {
		TYPES = new GSEChannelEntryType[values().length];
		for (GSEChannelEntryType type : values())
			TYPES[type.index] = type;
	}
	
	private final int index;
	private final String name;
	
	private GSEChannelEntryType(int index, String name) {
		this.index = index;
		this.name = name;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getName() {
		return name;
	}

	public static GSEChannelEntryType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
