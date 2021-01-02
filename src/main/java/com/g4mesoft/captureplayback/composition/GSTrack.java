package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSTrack {

	private final UUID trackUUID;
	private String name;
	
	private final Map<UUID, GSTrackEntry> entries;
	
	private GSComposition parent;
	
	public GSTrack(UUID trackUUID, String name) {
		if (trackUUID == null)
			throw new IllegalArgumentException("trackUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.trackUUID = trackUUID;
		this.name = name;
		
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
	
	public UUID getTrackUUID() {
		return trackUUID;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.name = name;
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
	
	public static GSTrack read(PacketByteBuf buf) throws IOException {
		UUID trackUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSTrack track = new GSTrack(trackUUID, name);
		
		return track;
	}

	public static void write(PacketByteBuf buf, GSTrack track) throws IOException {
		buf.writeUuid(track.getTrackUUID());
		buf.writeString(track.getName());
	}
}
