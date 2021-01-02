package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;

import net.minecraft.util.PacketByteBuf;

public class GSTrack {

	public static final boolean DEFAULT_DISABLED = false;

	private final UUID trackUUID;
	private GSTrackInfo info;
	private boolean disabled;

	private final Map<UUID, GSTrackEntry> entries;

	private GSTimeline parent;

	public GSTrack(UUID trackUUID, GSTrackInfo info) {
		if (trackUUID == null)
			throw new IllegalArgumentException("Track UUID must not be null!");
		if (info == null)
			throw new IllegalArgumentException("Info must not be null!");

		this.trackUUID = trackUUID;
		this.info = info;
		disabled = DEFAULT_DISABLED;
		
		entries = new LinkedHashMap<>();

		parent = null;
	}
	
	public GSTimeline getParent() {
		return parent;
	}

	void setParent(GSTimeline parent) {
		if (this.parent != null)
			throw new IllegalStateException("Track already has a parent");
		this.parent = parent;
	}
	
	public void set(GSTrack other) {
		setInfo(other.getInfo());
		setDisabled(other.isDisabled());

		clearTrack();
		
		for (GSTrackEntry entry : other.getEntries()) {
			GSTrackEntry entryCopy = new GSTrackEntry(entry.getEntryUUID());
			entryCopy.set(entry);
			addEntrySilent(entryCopy);
			dispatchEntryAdded(entryCopy);
		}
	}
	
	private void clearTrack() {
		Iterator<GSTrackEntry> itr = entries.values().iterator();
		while (itr.hasNext()) {
			GSTrackEntry entry = itr.next();
			itr.remove();
			
			dispatchEntryRemoved(entry);
		}
	}

	public GSTrackEntry tryAddEntry(GSSignalTime startTime, GSSignalTime endTime) {
		return tryAddEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), startTime, endTime);
	}
	
	public GSTrackEntry tryAddEntry(UUID entryUUID, GSSignalTime startTime, GSSignalTime endTime) {
		if (entryUUID == null || hasEntryUUID(entryUUID))
			return null;
		if (isOverlappingEntries(startTime, endTime, null))
			return null;
		
		GSTrackEntry entry = new GSTrackEntry(entryUUID, startTime, endTime);
		addEntrySilent(entry);
		
		dispatchEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntrySilent(GSTrackEntry entry) {
		entry.setParent(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(GSTrackEntry entry) {
		return removeEntry(entry.getEntryUUID());
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSTrackEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			dispatchEntryRemoved(entry);
			return true;
		}
		
		return false;
	}
	
	public boolean isOverlappingEntries(GSSignalTime startTime, GSSignalTime endTime, GSTrackEntry ignoreEntry) {
		if (startTime.isAfter(endTime))
			return false;
		
		for (GSTrackEntry other : entries.values()) {
			if (other != ignoreEntry && other.isOverlapping(startTime, endTime))
				return true;
		}
		return false;
	}
	
	public GSTrackEntry getEntryAt(GSSignalTime time, boolean preciseSearch) {
		for (GSTrackEntry entry : entries.values()) {
			if (entry.containsTimestamp(time, preciseSearch))
				return entry;
		}
		
		return null;
	}

	public void setInfo(GSTrackInfo info) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		
		GSTrackInfo oldInfo = this.info;
		if (!oldInfo.equals(info)) {
			this.info = info;
			
			dispatchTrackInfoChanged(this, oldInfo);
		}
	}
	
	public GSTrackInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		boolean oldDisabled = this.disabled;
		if (oldDisabled != disabled) {
			this.disabled = disabled;
			
			dispatchTrackDisabledChanged(this, oldDisabled);
		}
	}
	
	public boolean isDisabled() {
		return disabled;
	}

	public UUID getTrackUUID() {
		return trackUUID;
	}
	
	public GSTrackEntry getEntry(UUID entryUUID) {
		return entries.get(entryUUID);
	}
	
	public boolean hasEntryUUID(UUID entryUUID) {
		return entries.containsKey(entryUUID);
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<GSTrackEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
	
	private void dispatchTrackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
		if (parent != null) {
			for (GSITimelineListener listener : parent.getListeners())
				listener.trackInfoChanged(track, oldInfo);
		}
	}

	private void dispatchTrackDisabledChanged(GSTrack track, boolean oldDisabled) {
		if (parent != null) {
			for (GSITimelineListener listener : parent.getListeners())
				listener.trackDisabledChanged(track, oldDisabled);
		}
	}
	
	private void dispatchEntryAdded(GSTrackEntry entry) {
		if (parent != null) {
			for (GSITimelineListener listener : parent.getListeners())
				listener.entryAdded(entry);
		}
	}
	
	private void dispatchEntryRemoved(GSTrackEntry entry) {
		if (parent != null) {
			for (GSITimelineListener listener : parent.getListeners())
				listener.entryRemoved(entry);
		}
	}
	
	public static GSTrack read(PacketByteBuf buf) throws IOException {
		UUID trackUUID = buf.readUuid();
		GSTrackInfo info = GSTrackInfo.read(buf);
		GSTrack track = new GSTrack(trackUUID, info);

		track.setDisabled(buf.readBoolean());
		
		int entryCount = buf.readInt();
		while (entryCount-- != 0) {
			GSTrackEntry entry = GSTrackEntry.read(buf);
			if (track.hasEntryUUID(entry.getEntryUUID()))
				throw new IOException("Duplicate entry UUID");
			if (track.isOverlappingEntries(entry.getStartTime(), entry.getEndTime(), null))
				throw new IOException("Overlapping entry");
			track.addEntrySilent(entry);
		}
		
		return track;
	}

	public static void write(PacketByteBuf buf, GSTrack track) throws IOException {
		buf.writeUuid(track.getTrackUUID());
		GSTrackInfo.write(buf, track.getInfo());
		
		buf.writeBoolean(track.isDisabled());
		
		Collection<GSTrackEntry> entries = track.getEntries();
		buf.writeInt(entries.size());
		for (GSTrackEntry entry : entries)
			GSTrackEntry.write(buf, entry);
	}
}
