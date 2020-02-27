package com.g4mesoft.planner.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSTimeline {

	private final GSTimelineInfo info;
	private final List<GSTimelineEntry> entries;
	
	private boolean disabled;
	
	public GSTimeline(GSTimelineInfo info) {
		this.info = info;
		
		entries = new ArrayList<GSTimelineEntry>();
	}
	
	public boolean tryAddEntry(GSTimelineEntry entry) {
		if (isOverlappingEntries(entry.getStartTime(), entry.getEndTime(), null))
			return false;
		
		entries.add(entry);
		
		return true;
	}
	
	public boolean isOverlappingEntries(GSBlockEventTime startTime, GSBlockEventTime endTime, GSTimelineEntry ignoreEntry) {
		if (startTime.isAfter(endTime))
			return false;
		
		for (GSTimelineEntry other : entries) {
			if (other != ignoreEntry && other.isOverlapping(startTime, endTime))
				return true;
		}
		return false;
	}
	
	public GSTimelineEntry getEntryAt(GSBlockEventTime time, boolean preciseSearch) {
		for (GSTimelineEntry entry : entries) {
			if (entry.containsTimestamp(time, preciseSearch))
				return entry;
		}
		
		return null;
	}
	
	public GSTimelineInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public List<GSTimelineEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}
}
