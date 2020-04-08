package com.g4mesoft.planner.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSTrack {

	public static final int PROPERTY_INFO = 0;
	public static final int PROPERTY_DISABLED = 1;
	
	private GSTrackInfo info;
	private final GSTimeline timeline;

	private final List<GSTrackEntry> entries;

	private boolean disabled;
	
	public GSTrack(GSTrackInfo info, GSTimeline timeline) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		if (timeline == null)
			throw new NullPointerException("Timeline must not be null!");

		this.info = info;
		this.timeline = timeline;
		
		entries = new ArrayList<GSTrackEntry>();
		
		disabled = false;
	}
	
	public GSTrackEntry tryAddEntry(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (isOverlappingEntries(startTime, endTime, null))
			return null;
		
		GSTrackEntry entry = new GSTrackEntry(this, startTime, endTime);
		entries.add(entry);
		
		timeline.onEntryAdded(this, entry);
		
		return entry;
	}
	
	public boolean removeEntry(GSTrackEntry entry) {
		if (entries.remove(entry)) {
			timeline.onEntryRemoved(this, entry);
			return true;
		}
		
		return false;
	}
	
	public boolean isOverlappingEntries(GSBlockEventTime startTime, GSBlockEventTime endTime, GSTrackEntry ignoreEntry) {
		if (startTime.isAfter(endTime))
			return false;
		
		for (GSTrackEntry other : entries) {
			if (other != ignoreEntry && other.isOverlapping(startTime, endTime))
				return true;
		}
		return false;
	}
	
	public GSTrackEntry getEntryAt(GSBlockEventTime time, boolean preciseSearch) {
		for (GSTrackEntry entry : entries) {
			if (entry.containsTimestamp(time, preciseSearch))
				return entry;
		}
		
		return null;
	}

	void onEntryPropertyChanged(GSTrackEntry entry, int property) {
		timeline.onEntryPropertyChanged(this, entry, property);
	}
	
	public void setInfo(GSTrackInfo info) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		
		if (!this.info.equals(info)) {
			this.info = info;
			
			timeline.onTrackPropertyChanged(this, PROPERTY_INFO);
		}
	}
	
	public GSTrackInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		if (this.disabled != disabled) {
			this.disabled = disabled;
			
			timeline.onTrackPropertyChanged(this, PROPERTY_DISABLED);
		}
	}

	public boolean isDisabled() {
		return disabled;
	}

	public List<GSTrackEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}
}
