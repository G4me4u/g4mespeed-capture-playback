package com.g4mesoft.captureplayback.playlist;

public enum GSEPlaylistEntryType implements GSIPlaylistEntryType {

	START_PLAYBACK("startPlayback", 0, GSAssetPlaylistData.class),
	DELAY("delay", 0, GSDelayPlaylistData.class);
	
	private final String name;
	private final int index;
	private final Class<? extends GSIPlaylistData> dataClazz;
	
	public static final GSEPlaylistEntryType[] TYPES;
	
	static {
		TYPES = new GSEPlaylistEntryType[values().length];
		for (GSEPlaylistEntryType type : values())
			TYPES[type.index] = type;
	}
	
	private GSEPlaylistEntryType(String name, int index, Class<? extends GSIPlaylistData> dataClazz) {
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

	public static GSEPlaylistEntryType fromIndex(int index) {
		if (index < 0 || index >= TYPES.length)
			return null;
		return TYPES[index];
	}
}
