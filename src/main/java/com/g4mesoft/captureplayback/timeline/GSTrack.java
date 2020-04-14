package com.g4mesoft.captureplayback.timeline;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;

public class GSTrack {

	private final UUID trackUUID;
	private final GSTimeline timeline;
	private GSTrackInfo info;

	private final Map<UUID, GSTrackEntry> entries;

	private boolean disabled;
	
	public GSTrack(UUID trackUUID, GSTimeline timeline, GSTrackInfo info) {
		if (trackUUID == null)
			throw new NullPointerException("Track UUID must not be null!");
		if (timeline == null)
			throw new NullPointerException("Timeline must not be null!");
		if (info == null)
			throw new NullPointerException("Info must not be null!");

		this.trackUUID = trackUUID;
		this.timeline = timeline;
		this.info = info;
		
		entries = new LinkedHashMap<UUID, GSTrackEntry>();
		
		disabled = false;
	}

	public GSTrackEntry tryAddEntry(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		return tryAddEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), startTime, endTime);
	}
	
	public GSTrackEntry tryAddEntry(UUID entryUUID, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (entryUUID == null || hasEntryUUID(entryUUID))
			return null;
		if (isOverlappingEntries(startTime, endTime, null))
			return null;
		
		GSTrackEntry entry = new GSTrackEntry(entryUUID, this, startTime, endTime);
		entries.put(entryUUID, entry);
		
		timeline.onEntryAdded(entry);
		
		return entry;
	}
	
	public boolean removeEntry(GSTrackEntry entry) {
		return removeEntry(entry.getEntryUUID());
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSTrackEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			timeline.onEntryRemoved(entry);
			return true;
		}
		
		return false;
	}
	
	public boolean isOverlappingEntries(GSBlockEventTime startTime, GSBlockEventTime endTime, GSTrackEntry ignoreEntry) {
		if (startTime.isAfter(endTime))
			return false;
		
		for (GSTrackEntry other : entries.values()) {
			if (other != ignoreEntry && other.isOverlapping(startTime, endTime))
				return true;
		}
		return false;
	}
	
	public GSTrackEntry getEntryAt(GSBlockEventTime time, boolean preciseSearch) {
		for (GSTrackEntry entry : entries.values()) {
			if (entry.containsTimestamp(time, preciseSearch))
				return entry;
		}
		
		return null;
	}

	void onEntryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
		timeline.onEntryTimeChanged(entry, oldStart, oldEnd);
	}
	
	void onEntryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
		timeline.onEntryTypeChanged(entry, oldType);
	}
	
	public void setInfo(GSTrackInfo info) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		
		GSTrackInfo oldInfo = this.info;
		if (!oldInfo.equals(info)) {
			this.info = info;
			
			timeline.onTrackInfoChanged(this, oldInfo);
		}
	}
	
	public GSTrackInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		boolean oldDisabled = this.disabled;
		if (oldDisabled != disabled) {
			this.disabled = disabled;
			
			timeline.onTrackDisabledChanged(this, oldDisabled);
		}
	}
	
	public boolean isDisabled() {
		return disabled;
	}

	public UUID getTrackUUID() {
		return trackUUID;
	}
	
	public GSTimeline getTimeline() {
		return timeline;
	}
	
	public GSTrackEntry getEntry(UUID entryUUID) {
		return entries.get(entryUUID);
	}
	
	public boolean hasEntryUUID(UUID entryUUID) {
		return entries.containsKey(entryUUID);
	}
	
	public Set<Map.Entry<UUID, GSTrackEntry>> getEntryEntries() {
		return Collections.unmodifiableSet(entries.entrySet());
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<GSTrackEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
}
