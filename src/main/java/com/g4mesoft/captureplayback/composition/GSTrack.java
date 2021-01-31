package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSTrack {

	private final UUID trackUUID;
	private String name;
	private int color;
	
	private final Map<UUID, GSTrackEntry> entries;
	
	private GSComposition parent;
	
	public GSTrack(UUID trackUUID, String name, int color) {
		if (trackUUID == null)
			throw new IllegalArgumentException("trackUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.trackUUID = trackUUID;
		this.name = name;
		this.color = color | 0xFF000000;
		
		entries = new LinkedHashMap<>();
		
		parent = null;
	}
	
	public GSComposition getParent() {
		return parent;
	}

	void setParent(GSComposition parent) {
		if (this.parent != null)
			throw new IllegalStateException("Track already has a parent");
		this.parent = parent;
	}
	
	public GSTrackEntry addEntry(UUID sequenceUUID, long offset) {
		return addEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), sequenceUUID, offset);
	}
	
	public GSTrackEntry addEntry(UUID entryUUID, UUID sequenceUUID, long offset) {
		if (hasEntryUUID(entryUUID))
			throw new IllegalStateException("Duplicate entry UUID");
		if (parent == null || !parent.hasSequenceUUID(sequenceUUID))
			throw new IllegalStateException("Unknown sequence UUID");
		
		GSTrackEntry entry = new GSTrackEntry(entryUUID, sequenceUUID, offset);
		addEntryInternal(entry);
		
		dispatchEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntryInternal(GSTrackEntry entry) {
		entry.setParent(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSTrackEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			dispatchEntryRemoved(entry);
			return true;
		}
		
		return false;
	}
	
	public UUID getTrackUUID() {
		return trackUUID;
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
			
			dispatchTrackNameChanged(oldName);
		}
	}
	
	public int getColor() {
		return color;
	}
	
	public void setColor(int color) {
		// Ensure alpha channel is 255
		color |= 0xFF000000;
		
		if (color != this.color) {
			int oldColor = this.color;
			this.color = color;
			
			dispatchTrackColorChanged(oldColor);
		}
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
	
	public boolean isComplete() {
		if (parent == null)
			return false;
		
		for (GSTrackEntry entry : getEntries()) {
			if (!parent.hasSequenceUUID(entry.getSequenceUUID()))
				return false;
		}
		
		return true;
	}
	
	private void dispatchTrackNameChanged(String oldName) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.trackNameChanged(this, oldName);
		}
	}

	private void dispatchTrackColorChanged(int oldColor) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.trackColorChanged(this, oldColor);
		}
	}

	private void dispatchEntryAdded(GSTrackEntry entry) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.entryAdded(entry);
		}
	}
	
	private void dispatchEntryRemoved(GSTrackEntry entry) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.entryRemoved(entry);
		}
	}
	
	public static GSTrack read(PacketByteBuf buf) throws IOException {
		UUID trackUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		int color = buf.readMedium();
		GSTrack track = new GSTrack(trackUUID, name, color);

		int entryCount = buf.readInt();
		while (entryCount-- != 0) {
			GSTrackEntry entry = GSTrackEntry.read(buf);
			if (track.hasEntryUUID(entry.getEntryUUID()))
				throw new IOException("Duplicate entry UUID");
			track.addEntryInternal(entry);
		}
		
		return track;
	}

	public static void write(PacketByteBuf buf, GSTrack track) throws IOException {
		buf.writeUuid(track.getTrackUUID());
		buf.writeString(track.getName());
		buf.writeMedium(track.getColor());
		
		Collection<GSTrackEntry> entries = track.getEntries();
		buf.writeInt(entries.size());
		for (GSTrackEntry entry : entries)
			GSTrackEntry.write(buf, entry);
	}
}
