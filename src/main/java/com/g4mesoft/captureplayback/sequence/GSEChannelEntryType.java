package com.g4mesoft.captureplayback.sequence;

public enum GSEChannelEntryType {

	EVENT_BOTH(0, "panel.sequencecontent.typeboth"),
	EVENT_START(1, "panel.sequencecontent.typestart"),
	EVENT_END(2, "panel.sequencecontent.typeend"),
	EVENT_NONE(3, "panel.sequencecontent.typenone");

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
	
	public boolean hasStartEvent() {
		return (this == EVENT_BOTH || this == EVENT_START);
	}

	public boolean hasEndEvent() {
		return (this == EVENT_BOTH || this == EVENT_END);
	}

	public GSEChannelEntryType toggleStart() {
		switch (this) {
		case EVENT_BOTH:
			return GSEChannelEntryType.EVENT_END;
		case EVENT_START:
			return GSEChannelEntryType.EVENT_NONE;
		case EVENT_END:
			return GSEChannelEntryType.EVENT_BOTH;
		case EVENT_NONE:
			return GSEChannelEntryType.EVENT_START;
		}
		
		throw new IllegalStateException("Missing type");
	}

	public GSEChannelEntryType toggleEnd() {
		switch (this) {
		case EVENT_BOTH:
			return GSEChannelEntryType.EVENT_START;
		case EVENT_START:
			return GSEChannelEntryType.EVENT_BOTH;
		case EVENT_END:
			return GSEChannelEntryType.EVENT_NONE;
		case EVENT_NONE:
			return GSEChannelEntryType.EVENT_END;
		}
		
		throw new IllegalStateException("Missing type");
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
