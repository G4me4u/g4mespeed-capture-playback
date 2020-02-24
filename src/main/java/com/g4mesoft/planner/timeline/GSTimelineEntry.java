package com.g4mesoft.planner.timeline;

public class GSTimelineEntry {

	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private GSETimelineEntryType type;
	private boolean disabled;

	public GSTimelineEntry() {
		this(GSBlockEventTime.ZERO, GSBlockEventTime.ZERO);
	}
	
	public GSTimelineEntry(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		setTimespan(startTime, endTime);
	
		type = GSETimelineEntryType.EVENT_BOTH;
	}
	
	public void setTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after end time!");
			
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public long getGameTimeDuration() {
		return endTime.getGameTime() - startTime.getGameTime();
	}

	public void setStartTime(GSBlockEventTime startTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after current end time!");
		this.startTime = startTime;
	}
	
	public GSBlockEventTime getStartTime() {
		return startTime;
	}

	public void setEndTime(GSBlockEventTime endTime) {
		if (endTime.isBefore(startTime))
			throw new IllegalArgumentException("End time is before current start time!");
		this.endTime = endTime;
	}
	
	public boolean isOverlapping(GSTimelineEntry other) {
		if (other.startTime.isBefore(startTime) && other.endTime.isAfter(startTime))
			return true;
		if (other.startTime.isBefore(endTime) && other.endTime.isAfter(endTime))
			return true;
		return false;
	}
	
	public boolean containsTimestamp(GSBlockEventTime time) {
		return !startTime.isAfter(time) && endTime.isAfter(time);
	}
	
	public GSBlockEventTime getEndTime() {
		return endTime;
	}
	
	public void setType(GSETimelineEntryType type) {
		if (type == null)
			throw new NullPointerException();
		this.type = type;
	}
	
	public GSETimelineEntryType getType() {
		return type;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
}
