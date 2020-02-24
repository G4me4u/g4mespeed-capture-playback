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
		if (isOverlappingEntries(entry))
			return false;
		
		entries.add(entry);
		
		return true;
	}
	
	private boolean isOverlappingEntries(GSTimelineEntry entry) {
		for (GSTimelineEntry other : entries) {
			if (other.isOverlapping(entry))
				return true;
		}
		return false;
	}
	
	public GSTimelineEntry getEntryAt(GSBlockEventTime time) {
		for (GSTimelineEntry entry : entries) {
			if (entry.containsTimestamp(time))
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
