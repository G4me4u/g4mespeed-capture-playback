package com.g4mesoft.captureplayback.timeline;

import java.util.UUID;

public final class GSTrackEntry {

	public static final GSETrackEntryType DEFAULT_ENTRY_TYPE = GSETrackEntryType.EVENT_BOTH;
	
	private final UUID entryUUID;
	private final GSTrack track;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private GSETrackEntryType type;

	GSTrackEntry(UUID entryUUID, GSTrack track, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (entryUUID == null)
			throw new NullPointerException("entryUUID is null");
		if (track == null)
			throw new NullPointerException("track is null");
		
		this.entryUUID = entryUUID;
		this.track = track;
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		type = DEFAULT_ENTRY_TYPE;

		validateTimespan(startTime, endTime);
	}
	
	private void validateTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after end time!");
		if (track.isOverlappingEntries(startTime, endTime, this))
			throw new IllegalArgumentException("Timespan is overlapping other track entries!");
	}
	
	public void setTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		GSBlockEventTime oldStartTime = this.startTime;
		GSBlockEventTime oldEndTime = this.endTime;
		if (!oldStartTime.isEqual(startTime) || !oldEndTime.isEqual(endTime)) {
			validateTimespan(startTime, endTime);

			this.startTime = startTime;
			this.endTime = endTime;
			
			track.onEntryTimeChanged(this, oldStartTime, oldEndTime);
		}
	}
	
	public long getGametickDuration() {
		return endTime.getGametick() - startTime.getGametick();
	}

	public void setStartTime(GSBlockEventTime startTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after current end time!");
		setTimespan(startTime, this.endTime);
	}
	
	public GSBlockEventTime getStartTime() {
		return startTime;
	}

	public void setEndTime(GSBlockEventTime endTime) {
		if (endTime.isBefore(startTime))
			throw new IllegalArgumentException("End time is before current start time!");
		setTimespan(this.startTime, endTime);
	}
	
	public boolean isOverlapping(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		return !startTime.isAfter(this.endTime) && !endTime.isBefore(this.startTime);
	}
	
	public boolean containsTimestamp(GSBlockEventTime time, boolean includeBlockEventDelay) {
		if (includeBlockEventDelay)
			return !startTime.isAfter(time) && !endTime.isBefore(time);
		
		return time.getGametick() >= startTime.getGametick() &&
		       time.getGametick() <= endTime.getGametick();
	}
	
	public GSBlockEventTime getEndTime() {
		return endTime;
	}
	
	public void setType(GSETrackEntryType type) {
		if (type == null)
			throw new NullPointerException();
		
		GSETrackEntryType oldType = this.type;
		if (type != oldType) {
			this.type = type;
			
			track.onEntryTypeChanged(this, oldType);
		}
	}
	
	public GSETrackEntryType getType() {
		return type;
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}
	
	public GSTrack getTrack() {
		return track;
	}
}
