package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;

import net.minecraft.util.PacketByteBuf;

public class GSTrack {

	public static final boolean DEFAULT_DISABLED = false;

	private final UUID trackUUID;
	private GSTrackInfo info;
	private boolean disabled;

	private final Map<UUID, GSTrackEntry> entries;

	private GSTimeline owner;

	public GSTrack(UUID trackUUID, GSTrackInfo info) {
		if (trackUUID == null)
			throw new NullPointerException("Track UUID must not be null!");
		if (info == null)
			throw new NullPointerException("Info must not be null!");

		this.trackUUID = trackUUID;
		this.info = info;
		disabled = DEFAULT_DISABLED;
		
		entries = new LinkedHashMap<UUID, GSTrackEntry>();

		owner = null;
	}
	
	void setOwnerTimeline(GSTimeline owner) {
		if (this.owner != null)
			throw new IllegalStateException("Track already has an owner.");
		this.owner = owner;
	}
	
	public GSTimeline getOwnerTimeline() {
		return owner;
	}
	
	public void set(GSTrack other) {
		setInfo(other.getInfo());
		setDisabled(other.isDisabled());

		clearTrack();
		
		for (GSTrackEntry entry : other.getEntries()) {
			GSTrackEntry entryCopy = new GSTrackEntry(entry.getEntryUUID());
			entryCopy.set(entry);
			addEntrySilent(entryCopy);
			
			if (owner != null)
				owner.onEntryAdded(entryCopy);
		}
	}
	
	private void clearTrack() {
		Iterator<GSTrackEntry> itr = entries.values().iterator();
		while (itr.hasNext()) {
			GSTrackEntry entry = itr.next();
			itr.remove();
			
			if (owner != null)
				owner.onEntryRemoved(entry);
		}
	}

	public GSTrackEntry tryAddEntry(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		return tryAddEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), startTime, endTime);
	}
	
	public GSTrackEntry tryAddEntry(UUID entryUUID, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (entryUUID == null || hasEntryUUID(entryUUID))
			return null;
		if (isOverlappingEntries(startTime, endTime, null))
			return null;
		
		GSTrackEntry entry = new GSTrackEntry(entryUUID, startTime, endTime);
		addEntrySilent(entry);
		
		if (owner != null)
			owner.onEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntrySilent(GSTrackEntry entry) {
		entry.setOwnerTrack(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(GSTrackEntry entry) {
		return removeEntry(entry.getEntryUUID());
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSTrackEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			if (owner != null)
				owner.onEntryRemoved(entry);
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
		if (owner != null)
			owner.onEntryTimeChanged(entry, oldStart, oldEnd);
	}
	
	void onEntryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
		if (owner != null)
			owner.onEntryTypeChanged(entry, oldType);
	}
	
	public void setInfo(GSTrackInfo info) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		
		GSTrackInfo oldInfo = this.info;
		if (!oldInfo.equals(info)) {
			this.info = info;
			
			if (owner != null)
				owner.onTrackInfoChanged(this, oldInfo);
		}
	}
	
	public GSTrackInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		boolean oldDisabled = this.disabled;
		if (oldDisabled != disabled) {
			this.disabled = disabled;
			
			if (owner != null)
				owner.onTrackDisabledChanged(this, oldDisabled);
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
	
	public Set<Map.Entry<UUID, GSTrackEntry>> getEntryEntries() {
		return Collections.unmodifiableSet(entries.entrySet());
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<GSTrackEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
	
	public static GSTrack read(PacketByteBuf buf) throws IOException {
		UUID trackUUID = buf.readUuid();
		GSTrackInfo info = GSTrackInfo.read(buf);
		GSTrack track = new GSTrack(trackUUID, info);

		track.setDisabled(buf.readBoolean());
		
		int numEntries = buf.readInt();
		while (numEntries-- != 0) {
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
