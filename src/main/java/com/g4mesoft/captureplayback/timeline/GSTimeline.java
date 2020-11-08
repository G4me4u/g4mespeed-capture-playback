package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSTimeline {

	private String name;
	
	private final Map<UUID, GSTrack> tracks;
	private final List<GSITimelineListener> listeners;
	
	public GSTimeline() {
		tracks = new LinkedHashMap<>();
		listeners = new ArrayList<>();
	}

	public void set(GSTimeline other) {
		setName(other.getName());
		
		clearTimeline();
		
		for (GSTrack track : other.getTracks()) {
			GSTrack trackCopy = new GSTrack(track.getTrackUUID(), track.getInfo());
			trackCopy.set(track);
			addTrackSilent(trackCopy);
			
			dispatchTrackAdded(trackCopy);
		}
	}
	
	private void clearTimeline() {
		Iterator<GSTrack> itr = tracks.values().iterator();
		while (itr.hasNext()) {
			GSTrack track = itr.next();
			itr.remove();
			
			dispatchTrackRemoved(track);
		}
	}
	
	public GSTrack addTrack(GSTrackInfo info) {
		return addTrack(GSUUIDUtil.randomUnique(this::hasTrackUUID), info);
	}
	
	public GSTrack addTrack(UUID trackUUID, GSTrackInfo info) {
		if (hasTrackUUID(trackUUID))
			throw new IllegalStateException("Duplicate track UUID");
		
		GSTrack track = new GSTrack(trackUUID, info);
		addTrackSilent(track);
		
		dispatchTrackAdded(track);
		
		return track;
	}
	
	private void addTrackSilent(GSTrack track) {
		track.setOwnerTimeline(this);
		
		tracks.put(track.getTrackUUID(), track);
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
	
	void onEntryTimeChanged(GSTrackEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
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
	
	public static GSTimeline read(PacketByteBuf buf) throws IOException {
		GSTimeline timeline = new GSTimeline();

		if (buf.readBoolean())
			timeline.setName(buf.readString(GSBufferUtil.MAX_STRING_LENGTH));
		
		int numTracks = buf.readInt();
		while (numTracks-- != 0) {
			GSTrack track = GSTrack.read(buf);
			if (timeline.hasTrackUUID(track.getTrackUUID()))
				throw new IOException("Duplicate track UUID.");
			timeline.addTrackSilent(track);
		}

		return timeline;
	}

	public static void write(PacketByteBuf buf, GSTimeline timeline) throws IOException {
		if (timeline.getName() != null) {
			buf.writeBoolean(true);
			buf.writeString(timeline.getName());
		} else {
			buf.writeBoolean(false);
		}
		
		Collection<GSTrack> tracks = timeline.getTracks();
		buf.writeInt(tracks.size());
		for (GSTrack track : tracks)
			GSTrack.write(buf, track);
	}
}
