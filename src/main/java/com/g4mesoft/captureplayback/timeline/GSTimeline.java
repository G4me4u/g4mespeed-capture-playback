package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSTimeline {

	private final UUID timelineUUID;
	private String name;
	
	private final Map<UUID, GSTrack> tracks;
	private final List<GSITimelineListener> listeners;

	public GSTimeline(UUID timelineUUID) {
		this(timelineUUID, "");
	}

	public GSTimeline(UUID timelineUUID, String name) {
		if (timelineUUID == null)
			throw new IllegalArgumentException("timelineUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.timelineUUID = timelineUUID;
		this.name = name;
		
		tracks = new LinkedHashMap<>();
		listeners = new ArrayList<>();
	}

	public void set(GSTimeline other) {
		setName(other.getName());
		
		clearTimeline();
		
		for (GSTrack track : other.getTracks()) {
			GSTrack trackCopy = new GSTrack(track.getTrackUUID(), track.getInfo());
			trackCopy.set(track);
			addTrackInternal(trackCopy);
			
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
		addTrackInternal(track);
		
		dispatchTrackAdded(track);
		
		return track;
	}
	
	private void addTrackInternal(GSTrack track) {
		track.setParent(this);
		
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
	
	public UUID getTimelineUUID() {
		return timelineUUID;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		if (!name.equals(this.name)) {
			String oldName = this.name;
			this.name = name;
			
			dispatchTimelineNameChanged(oldName);
		}
	}

	public GSTrack getTrack(UUID trackUUID) {
		return tracks.get(trackUUID);
	}
	
	public boolean hasTrackUUID(UUID trackUUID) {
		return tracks.containsKey(trackUUID);
	}
	
	public Set<UUID> getTrackUUIDs() {
		return Collections.unmodifiableSet(tracks.keySet());
	}
	
	public Collection<GSTrack> getTracks() {
		return Collections.unmodifiableCollection(tracks.values());
	}
	
	public void addTimelineListener(GSITimelineListener listener) {
		listeners.add(listener);
	}

	public void removeTimelineListener(GSITimelineListener listener) {
		listeners.remove(listener);
	}
	
	/* Visible to allow events from the tracks and entries */
	Iterable<GSITimelineListener> getListeners() {
		return listeners;
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
	
	public static GSTimeline read(PacketByteBuf buf) throws IOException {
		// Skip reserved byte
		buf.readByte();

		UUID timelineUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSTimeline timeline = new GSTimeline(timelineUUID, name);

		int trackCount = buf.readInt();
		while (trackCount-- != 0) {
			GSTrack track = GSTrack.read(buf);
			if (timeline.hasTrackUUID(track.getTrackUUID()))
				throw new IOException("Duplicate track UUID.");
			timeline.addTrackInternal(track);
		}

		return timeline;
	}

	public static void write(PacketByteBuf buf, GSTimeline timeline) throws IOException {
		// Reserved for future use
		buf.writeByte(0x00);
		
		buf.writeUuid(timeline.getTimelineUUID());
		buf.writeString(timeline.getName());
		
		Collection<GSTrack> tracks = timeline.getTracks();
		buf.writeInt(tracks.size());
		for (GSTrack track : tracks)
			GSTrack.write(buf, track);
	}
}
