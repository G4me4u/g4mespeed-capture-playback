package com.g4mesoft.captureplayback.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;

public class GSTimeline {

	private String name;
	
	private final Map<UUID, GSTrack> tracks;
	private final List<GSITimelineListener> listeners;
	
	public GSTimeline() {
		tracks = new LinkedHashMap<UUID, GSTrack>();
		listeners = new ArrayList<GSITimelineListener>();
	}

	public void addTrack(GSTrackInfo info) {
		addTrack(GSUUIDUtil.randomUnique(this::hasTrackUUID), info);
	}
	
	public void addTrack(UUID trackUUID, GSTrackInfo info) {
		if (hasTrackUUID(trackUUID))
			throw new IllegalStateException("Duplicate track UUID");
		
		GSTrack track = new GSTrack(trackUUID, this, info);
		tracks.put(trackUUID, track);
		
		dispatchTrackAdded(track);
	}
	
	public boolean removeTrack(UUID trackUUID) {
		GSTrack track = tracks.remove(trackUUID);
		if (track != null) {
			dispatchTrackRemoved(track);
			return true;
		}
		
		return false;
	}
	
	public void addTimelineListener(GSITimelineListener listener) {
		listeners.add(listener);
	}

	public void removeTimelineListener(GSITimelineListener listener) {
		listeners.remove(listener);
	}
	
	void onTrackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
		for (GSITimelineListener listener : listeners)
			listener.trackInfoChanged(track, oldInfo);
	}

	void onTrackDisabledChanged(GSTrack track, boolean oldDisabled) {
		for (GSITimelineListener listener : listeners)
			listener.trackDisabledChanged(track, oldDisabled);
	}

	void onEntryAdded(GSTrackEntry entry) {
		for (GSITimelineListener listener : listeners)
			listener.entryAdded(entry);
	}
	
	void onEntryRemoved(GSTrackEntry entry) {
		for (GSITimelineListener listener : listeners)
			listener.entryRemoved(entry);
	}
	
	void onEntryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
		for (GSITimelineListener listener : listeners)
			listener.entryTimeChanged(entry, oldStart, oldEnd);
	}
	
	void onEntryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
		for (GSITimelineListener listener : listeners)
			listener.entryTypeChanged(entry, oldType);
	}
	
	private void dispatchTimelineNameChanged(String oldName) {
		for (GSITimelineListener listener : listeners)
			listener.timelineNameChanged(oldName);
	}

	private void dispatchTrackAdded(GSTrack track) {
		for (GSITimelineListener listener : listeners)
			listener.trackAdded(track);
	}

	private void dispatchTrackRemoved(GSTrack track) {
		for (GSITimelineListener listener : listeners)
			listener.trackRemoved(track);
	}

	public void setName(String name) {
		String oldName = this.name;
		if (!Objects.equals(name, oldName)) {
			this.name = name;
			
			dispatchTimelineNameChanged(oldName);
		}
	}
	
	public String getName() {
		return name;
	}

	public GSTrack getTrack(UUID trackUUID) {
		return tracks.get(trackUUID);
	}
	
	public boolean hasTrackUUID(UUID trackUUID) {
		return tracks.containsKey(trackUUID);
	}
	
	public Set<Map.Entry<UUID, GSTrack>> getTrackEntries() {
		return Collections.unmodifiableSet(tracks.entrySet());
	}

	public Set<UUID> getTrackUUIDs() {
		return Collections.unmodifiableSet(tracks.keySet());
	}
	
	public Collection<GSTrack> getTracks() {
		return Collections.unmodifiableCollection(tracks.values());
	}
}
