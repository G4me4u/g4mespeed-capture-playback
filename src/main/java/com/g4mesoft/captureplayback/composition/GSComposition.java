package com.g4mesoft.captureplayback.composition;

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

import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.captureplayback.util.GSMutableLinkedHashMap;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSComposition {

	private final UUID compositionUUID;
	private String name;

	private final Map<UUID, GSTrackGroup> groups;
	private final Map<UUID, GSTrack> tracks;
	
	private List<GSICompositionListener> listeners;

	public GSComposition(GSComposition other) {
		this(other.getCompositionUUID(), other.getName());
		
		for (GSTrackGroup group : other.getGroups())
			addGroupInternal(new GSTrackGroup(group));
		for (GSTrack track : other.getTracks())
			addTrackInternal(new GSTrack(track));
	}
	
	public GSComposition(UUID compositionUUID) {
		this(compositionUUID, "");
	}

	public GSComposition(UUID compositionUUID, String name) {
		if (compositionUUID == null)
			throw new IllegalArgumentException("compositionUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.compositionUUID = compositionUUID;
		this.name = name;
		
		groups = new GSMutableLinkedHashMap<>();
		// TODO: remove linked from hash map (once panel supports group rendering).
		tracks = new LinkedHashMap<>();
		
		listeners = null;
	}

	public void set(GSComposition other) {
		clear();

		setName(other.getName());
		
		for (GSTrackGroup group : other.getGroups())
			addGroup(group.getGroupUUID(), group.getName()).set(group);
		for (GSTrack track : other.getTracks())
			addTrack(track.getTrackUUID(), track.getName(), track.getColor(), track.getGroupUUID()).set(track);
	}
	
	private void clear() {
		Iterator<GSTrack> trackItr = tracks.values().iterator();
		while (trackItr.hasNext()) {
			GSTrack track = trackItr.next();
			trackItr.remove();
			onTrackRemoved(track);
		}

		Iterator<GSTrackGroup> groupItr = groups.values().iterator();
		while (groupItr.hasNext()) {
			GSTrackGroup group = groupItr.next();
			groupItr.remove();
			onGroupRemoved(group);
		}
	}
	
	public GSTrackGroup addGroup(String groupName) {
		return addGroup(GSUUIDUtil.randomUnique(this::hasGroupUUID), groupName);
	}
	
	public GSTrackGroup addGroup(UUID groupUUID, String groupName) {
		if (hasGroupUUID(groupUUID))
			throw new IllegalStateException("Duplicate sequence UUID");
	
		GSTrackGroup group = new GSTrackGroup(groupUUID, groupName);
		addGroupInternal(group);
	
		dispatchGroupAdded(group);
	
		return group;
	}
	
	private void addGroupInternal(GSTrackGroup group) {
		group.onAdded(this);
		
		groups.put(group.getGroupUUID(), group);
	}
	
	public boolean removeGroup(UUID groupUUID) {
		GSTrackGroup group = groups.get(groupUUID);

		if (group != null) {
			// Remove all tracks in the group first.
			for (UUID trackUUID : group.getTrackUUIDs())
				removeTrack(trackUUID);
			
			groups.remove(groupUUID);
			onGroupRemoved(group);
			return true;
		}
		
		return false;
	}
	
	private void onGroupRemoved(GSTrackGroup group) {
		dispatchGroupRemoved(group);
		group.onRemoved(this);
	}
	
	public GSTrack addTrack(String trackName, int color, UUID groupUUID) {
		return addTrack(GSUUIDUtil.randomUnique(this::hasTrackUUID), trackName, color, groupUUID);
	}
	
	public GSTrack addTrack(UUID trackUUID, String trackName, int color, UUID groupUUID) {
		if (hasTrackUUID(trackUUID))
			throw new IllegalStateException("Duplicate track UUID");
		if (!hasGroupUUID(groupUUID))
			throw new IllegalArgumentException("Group does not exist");
		
		GSTrack track = new GSTrack(trackUUID, trackName, color, groupUUID);
		addTrackInternal(track);
		
		dispatchTrackAdded(track);
		
		return track;
	}

	private void addTrackInternal(GSTrack track) {
		track.onAdded(this);
		
		tracks.put(track.getTrackUUID(), track);
	}
	
	public boolean removeTrack(UUID trackUUID) {
		GSTrack track = tracks.remove(trackUUID);
		if (track != null) {
			onTrackRemoved(track);
			return true;
		}
		
		return false;
	}
	
	private void onTrackRemoved(GSTrack track) {
		dispatchTrackRemoved(track);
		track.onRemoved(this);
	}

	public UUID getCompositionUUID() {
		return compositionUUID;
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
			
			dispatchCompositionNameChanged(oldName);
		}
	}
	
	public GSTrackGroup getGroup(UUID groupUUID) {
		return groups.get(groupUUID);
	}
	
	public boolean hasGroupUUID(UUID groupUUID) {
		return groups.containsKey(groupUUID);
	}

	public GSTrack getTrack(UUID trackUUID) {
		return tracks.get(trackUUID);
	}
	
	public boolean hasTrackUUID(UUID trackUUID) {
		return tracks.containsKey(trackUUID);
	}
	
	public Collection<GSTrackGroup> getGroups() {
		return Collections.unmodifiableCollection(groups.values());
	}

	public Set<UUID> getGroupUUIDs() {
		return Collections.unmodifiableSet(groups.keySet());
	}

	public Collection<GSTrack> getTracks() {
		return Collections.unmodifiableCollection(tracks.values());
	}
	
	public Set<UUID> getTrackUUIDs() {
		return Collections.unmodifiableSet(tracks.keySet());
	}
	
	public void addCompositionListener(GSICompositionListener listener) {
		if (listeners == null)
			listeners = new ArrayList<>();
		listeners.add(listener);
	}

	public void removeCompositionListener(GSICompositionListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}
	
	/* Visible to allow events from the tracks and entries */
	Iterable<GSICompositionListener> getListeners() {
		return (listeners == null) ? Collections.emptyList() : listeners;
	}
	
	private void dispatchCompositionNameChanged(String oldName) {
		for (GSICompositionListener listener : getListeners())
			listener.compositionNameChanged(oldName);
	}

	private void dispatchGroupAdded(GSTrackGroup group) {
		for (GSICompositionListener listener : getListeners())
			listener.groupAdded(group);
	}

	private void dispatchGroupRemoved(GSTrackGroup group) {
		for (GSICompositionListener listener : getListeners())
			listener.groupRemoved(group);
	}
	
	private void dispatchTrackAdded(GSTrack track) {
		for (GSICompositionListener listener : getListeners())
			listener.trackAdded(track);
	}

	private void dispatchTrackRemoved(GSTrack track) {
		for (GSICompositionListener listener : getListeners())
			listener.trackRemoved(track);
	}

	public static GSComposition read(PacketByteBuf buf) throws IOException {
		// Skip reserved byte
		buf.readByte();
		
		UUID compositionUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSComposition composition = new GSComposition(compositionUUID, name);

		int groupCount = buf.readInt();
		while (groupCount-- != 0) {
			GSTrackGroup group = GSTrackGroup.read(buf);
			if (composition.hasGroupUUID(group.getGroupUUID()))
				throw new IOException("Duplicate group UUID");
			composition.addGroupInternal(group);
		}

		int trackCount = buf.readInt();
		while (trackCount-- != 0) {
			GSTrack track = GSTrack.read(buf);
			if (composition.hasTrackUUID(track.getTrackUUID()))
				throw new IOException("Duplicate track UUID");
			composition.addTrackInternal(track);
		}
		
		return composition;
	}

	public static void write(PacketByteBuf buf, GSComposition composition) throws IOException {
		// Reserved byte for future use
		buf.writeByte(0x00);

		buf.writeUuid(composition.getCompositionUUID());
		buf.writeString(composition.getName());

		Collection<GSTrackGroup> groups = composition.getGroups();
		buf.writeInt(groups.size());
		for (GSTrackGroup group : groups)
			GSTrackGroup.write(buf, group);
		
		Collection<GSTrack> tracks = composition.getTracks();
		buf.writeInt(tracks.size());
		for (GSTrack track : tracks)
			GSTrack.write(buf, track);
	}

	public GSIPlaybackStream getPlaybackStream() {
		return new GSCompositionPlaybackStream(this);
	}
	
	public GSICaptureStream getCaptureStream() {
		return new GSCompositionCaptureStream(this);
	}
	
	/* Method visible for play-back & capture streams */
	GSBlockRegion getBlockRegion() {
		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int z0 = Integer.MAX_VALUE;

		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		int z1 = Integer.MIN_VALUE;
		
		for (GSTrack track : getTracks()) {
			GSBlockRegion region = track.getSequence().getBlockRegion();
			
			if (region.getX0() < x0)
				x0 = region.getX0();
			if (region.getY0() < y0)
				y0 = region.getY0();
			if (region.getZ0() < z0)
				z0 = region.getZ0();

			if (region.getX1() > x1)
				x1 = region.getX1();
			if (region.getY1() > y1)
				y1 = region.getY1();
			if (region.getZ1() > z1)
				z1 = region.getZ1();
		}
		
		return new GSBlockRegion(x0, y0, z0, x1, y1, z1);
	}
}
