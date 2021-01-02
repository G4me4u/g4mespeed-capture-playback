package com.g4mesoft.captureplayback.composition;

import java.util.UUID;

public class GSTrackEntry {

	private final UUID entryUUID;
	private final UUID sequenceUUID;
	private long offset;
	
	public GSTrackEntry(UUID entryUUID, UUID sequenceUUID, long offset) {
		this.entryUUID = entryUUID;
		this.sequenceUUID = sequenceUUID;

		setOffset(offset);
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}

	public UUID getSequenceUUID() {
		return sequenceUUID;
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
