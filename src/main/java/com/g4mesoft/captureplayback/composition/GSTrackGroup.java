package com.g4mesoft.captureplayback.composition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSMutableLinkedHashSet;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSTrackGroup {

	private final UUID groupUUID;
	private String name;
	
	private final GSMutableLinkedHashSet<UUID> trackUUIDs;
	
	private GSComposition parent;
	
	public GSTrackGroup(GSTrackGroup other) {
		this(other.getGroupUUID(), other.getName());
		
		for (UUID trackUUID : other.getTrackUUIDs())
			trackUUIDs.add(trackUUID);
	}
	
	public GSTrackGroup(UUID groupUUID, String name) {
		this.groupUUID = groupUUID;
		this.name = name;
		
		trackUUIDs = new GSMutableLinkedHashSet<>();
	
		parent = null;
	}
	
	public GSComposition getParent() {
		return parent;
	}
	
	void onAdded(GSComposition parent) {
		if (this.parent != null)
			throw new IllegalStateException("Group already has a parent");
		
		this.parent = parent;
	}

	void onRemoved(GSComposition parent) {
		if (this.parent != parent)
			throw new IllegalStateException("Group does not have the specified parent");
		
		this.parent = null;
	}
	
	void set(GSTrackGroup other) {
		setName(other.getName());
		
		trackUUIDs.clear();
		trackUUIDs.addAll(other.getTrackUUIDs());
	}
	
	/* Visible for GSTrack */
	void addTrack(UUID trackUUID) {
		trackUUIDs.add(trackUUID);
	}

	/* Visible for GSTrack */
	void removeTrack(UUID trackUUID) {
		trackUUIDs.remove(trackUUID);
	}
	
	public boolean hasTrackUUID(UUID trackUUID) {
		return trackUUIDs.contains(trackUUID);
	}
	
	public UUID getGroupUUID() {
		return groupUUID;
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
			
			dispatchNameChanged(oldName);
		}
	}
	
	private void dispatchNameChanged(String oldName) {
		if (parent != null) {
			for (GSICompositionListener listener : parent.getListeners())
				listener.groupNameChanged(this, oldName);
		}
	}
	
	public Set<UUID> getTrackUUIDs() {
		return Collections.unmodifiableSet(trackUUIDs);
	}

	public static GSTrackGroup read(PacketByteBuf buf) {
		UUID groupUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		
		GSTrackGroup group = new GSTrackGroup(groupUUID, name);
		
		int trackCount = buf.readInt();
		while (trackCount-- != 0)
			group.addTrack(buf.readUuid());
		
		return group;
	}

	public static void write(PacketByteBuf buf, GSTrackGroup group) {
		buf.writeUuid(group.getGroupUUID());
		buf.writeString(group.getName());
		
		Collection<UUID> trackUUIDs = group.getTrackUUIDs();
		buf.writeInt(trackUUIDs.size());
		for (UUID trackUUID : trackUUIDs)
			buf.writeUuid(trackUUID);
	}
}
