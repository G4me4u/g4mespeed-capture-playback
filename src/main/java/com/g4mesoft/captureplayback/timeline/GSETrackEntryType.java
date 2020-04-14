package com.g4mesoft.captureplayback.timeline;

public enum GSETrackEntryType {

	EVENT_BOTH(0),
	EVENT_START(1),
	EVENT_END(2);
	
	private final int index;
	
	private GSETrackEntryType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
}
