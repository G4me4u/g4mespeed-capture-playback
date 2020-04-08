package com.g4mesoft.planner.timeline;

public final class GSTrackEntry {

	public static final int PROPERTY_TIMESPAN = 0;
	public static final int PROPERTY_TYPE     = 1;
	public static final int PROPERTY_DISABLED = 2;
	
	private final GSTrack track;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private GSETrackEntryType type;
	private boolean disabled;

	GSTrackEntry(GSTrack track, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		this.track = track;
		
		setTimespan(startTime, endTime);
	
		type = GSETrackEntryType.EVENT_BOTH;
	}
	
	public void setTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after end time!");
		if (track.isOverlappingEntries(startTime, endTime, this))
			throw new IllegalArgumentException("Timespan is overlapping other track entries!");
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		track.onEntryPropertyChanged(this, PROPERTY_TIMESPAN);
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
		this.type = type;
		
		track.onEntryPropertyChanged(this, PROPERTY_TYPE);
	}
	
	public GSETrackEntryType getType() {
		return type;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
		
		track.onEntryPropertyChanged(this, PROPERTY_DISABLED);
	}
	
	public boolean isDisabled() {
		return disabled;
	}
}
