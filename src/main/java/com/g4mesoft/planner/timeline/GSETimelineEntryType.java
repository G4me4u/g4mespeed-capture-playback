package com.g4mesoft.planner.timeline;

public enum GSETimelineEntryType {

	EVENT_BOTH(0),
	EVENT_START(1),
	EVENT_END(2);
	
	private final int index;
	
	private GSETimelineEntryType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
}
