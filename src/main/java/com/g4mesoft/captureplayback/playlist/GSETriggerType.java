package com.g4mesoft.captureplayback.playlist;

public enum GSETriggerType implements GSIPlaylistEntryType {

	UNSPECIFIED("unspecified", 0, GSUnspecifiedPlaylistData.class),
	HOTKEY("hotkey", 1, GSHotkeyPlaylistData.class),
	ITEM_USE("itemUse", 2, GSUnspecifiedPlaylistData.class);
	
	private final String name;
	private final int index;
	private final Class<? extends GSIPlaylistData> dataClazz;
	
	public static final GSETriggerType[] TYPES;
	
	static {
		TYPES = new GSETriggerType[values().length];
		for (GSETriggerType type : values())
			TYPES[type.index] = type;
	}
	
	private GSETriggerType(String name, int index, Class<? extends GSIPlaylistData> dataClazz) {
		this.name = name;
		this.index = index;
		this.dataClazz = dataClazz;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public Class<? extends GSIPlaylistData> getDataClazz() {
		return dataClazz;
	}

	public static GSETriggerType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
