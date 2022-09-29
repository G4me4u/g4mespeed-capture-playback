package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSTrack {

	private final UUID trackUUID;
	private String name;
	private int color;
	private UUID groupUUID;
	
	private final GSSequence sequence;
	private final Map<UUID, GSTrackEntry> entries;
	
	private GSComposition parent;

	GSTrack(GSTrack other) {
		trackUUID = other.getTrackUUID();
		name = other.getName();
		color = other.getColor();
		groupUUID = other.getGroupUUID();
		
		sequence = new GSSequence(other.getSequence());
		entries = new LinkedHashMap<>();
		
		for (GSTrackEntry entry : other.getEntries())
			addEntryInternal(new GSTrackEntry(entry));
	}
	
	GSTrack(UUID trackUUID, String name, int color, UUID groupUUID) {
		this(trackUUID, name, color, groupUUID, new GSSequence(trackUUID));
	}
	
	private GSTrack(UUID trackUUID, String name, int color, UUID groupUUID, GSSequence sequence) {
		if (trackUUID == null)
			throw new IllegalArgumentException("trackUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		if (groupUUID == null)
			throw new IllegalArgumentException("groupUUID is null");
		if (sequence == null)
			throw new IllegalArgumentException("sequence is null");
		
		this.trackUUID = trackUUID;
		this.name = name;
		this.color = color | 0xFF000000;
		this.groupUUID = groupUUID;

		this.sequence = sequence;
		entries = new LinkedHashMap<>();
		
		parent = null;
	}
	
	public GSComposition getParent() {
		return parent;
	}

	void onAdded(GSComposition parent) {
		if (this.parent != null)
			throw new IllegalStateException("Track already has a parent");
		
		this.parent = parent;
		
		GSTrackGroup group = getGroup();
		if (group != null)
			group.addTrack(trackUUID);
	}

	void onRemoved(GSComposition parent) {
		if (this.parent != parent)
			throw new IllegalStateException("Track does not have the specified parent");

		GSTrackGroup group = getGroup();
		if (group != null)
			group.removeTrack(trackUUID);
		
		this.parent = null;
	}
	
	void set(GSTrack other) {
		clear();

		setName(other.getName());
		setColor(other.getColor());
		setGroupUUID(other.getGroupUUID());
		
		sequence.set(other.getSequence());
		for (GSTrackEntry entry : other.getEntries())
			addEntry(entry.getEntryUUID(), entry.getOffset()).set(entry);
	}
	
	private void clear() {
		Iterator<GSTrackEntry> itr = entries.values().iterator();
		while (itr.hasNext()) {
			GSTrackEntry entry = itr.next();
			itr.remove();
			onEntryRemoved(entry);
		}
	}
	
	public GSTrackEntry addEntry(long offset) {
		return addEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), offset);
	}
	
	public GSTrackEntry addEntry(UUID entryUUID, long offset) {
		if (hasEntryUUID(entryUUID))
			throw new IllegalStateException("Duplicate entry UUID");
		
		GSTrackEntry entry = new GSTrackEntry(entryUUID, offset);
		addEntryInternal(entry);
		
		dispatchEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntryInternal(GSTrackEntry entry) {
		entry.onAdded(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSTrackEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			onEntryRemoved(entry);
			return true;
		}
		
		return false;
	}
	
	private void onEntryRemoved(GSTrackEntry entry) {
		dispatchEntryRemoved(entry);
		entry.onRemoved(this);
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
	
	public UUID getGroupUUID() {
		return groupUUID;
	}

	public void setGroupUUID(UUID groupUUID) {
		if (parent == null || !parent.hasGroupUUID(groupUUID))
			throw new IllegalArgumentException("Group does not exist");
		
		if (!groupUUID.equals(this.groupUUID)) {
			UUID oldGroupUUID = this.groupUUID;
			
			getGroup().removeTrack(trackUUID);
			this.groupUUID = groupUUID;
			getGroup().addTrack(trackUUID);
			
			dispatchGroupChanged(oldGroupUUID);
		}
	}
	
	public GSTrackGroup getGroup() {
		if (parent == null)
			throw new IllegalStateException("Track not added");
		return parent.getGroup(groupUUID);
	}
	
	public GSSequence getSequence() {
		return sequence;
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
	
	private void dispatchGroupChanged(UUID oldGroup) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.trackGroupChanged(this, oldGroup);
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
		UUID groupUUID = buf.readUuid();
		
		GSSequence sequence = GSSequence.read(buf);
		
		GSTrack track = new GSTrack(trackUUID, name, color, groupUUID, sequence);

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
		buf.writeUuid(track.getGroupUUID());
		
		GSSequence.write(buf, track.getSequence());
		
		Collection<GSTrackEntry> entries = track.getEntries();
		buf.writeInt(entries.size());
		for (GSTrackEntry entry : entries)
			GSTrackEntry.write(buf, entry);
	}
}
