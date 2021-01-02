package com.g4mesoft.captureplayback.composition;

import java.util.UUID;

public class GSRocketEntry {

	private final UUID entryUUID;
	private final UUID timelineUUID;
	private long offset;
	
	public GSRocketEntry(UUID entryUUID, UUID timelineUUID, long offset) {
		this.entryUUID = entryUUID;
		this.timelineUUID = timelineUUID;

		setOffset(offset);
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}

	public UUID getTimelineUUID() {
		return timelineUUID;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public void setOffset(long offset) {
		if (offset < 0L)
			throw new IllegalArgumentException("offset must be non-negative");
		this.offset = offset;
	}
}
