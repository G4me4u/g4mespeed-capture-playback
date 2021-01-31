package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSComposition {

	private final UUID compositionUUID;
	private String name;
	
	private final Map<UUID, GSSequence> sequences;
	private final Map<UUID, GSTrack> tracks;
	
	private List<GSICompositionListener> listeners;

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
		
		sequences = new LinkedHashMap<>();
		tracks = new LinkedHashMap<>();
		
		listeners = null;
	}

	public void addSequence(GSSequence sequence) {
		if (hasSequenceUUID(sequence.getSequenceUUID()))
			throw new IllegalStateException("Duplicate sequence UUID");
	
		addSequenceInternal(sequence);
	
		dispatchSequenceAdded(sequence);
	}
	
	private void addSequenceInternal(GSSequence sequence) {
		sequences.put(sequence.getSequenceUUID(), sequence);
	}
	
	public boolean removeSequence(UUID sequenceUUID) {
		GSSequence sequence = sequences.remove(sequenceUUID);
		if (sequence != null) {
			dispatchSequenceRemoved(sequence);
			return true;
		}
		
		return false;
	}
	
	public GSTrack addTrack(String trackName, int color) {
		return addTrack(GSUUIDUtil.randomUnique(this::hasTrackUUID), trackName, color);
	}
	
	public GSTrack addTrack(UUID trackUUID, String trackName, int color) {
		if (hasTrackUUID(trackUUID))
			throw new IllegalStateException("Duplicate track UUID");
		
		GSTrack track = new GSTrack(trackUUID, trackName, color);
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
	
	public GSSequence getSequence(UUID sequenceUUID) {
		return sequences.get(sequenceUUID);
	}
	
	public boolean hasSequenceUUID(UUID sequenceUUID) {
		return sequences.containsKey(sequenceUUID);
	}

	public GSTrack getTrack(UUID trackUUID) {
		return tracks.get(trackUUID);
	}
	
	public boolean hasTrackUUID(UUID trackUUID) {
		return tracks.containsKey(trackUUID);
	}
	
	public Collection<GSSequence> getSequences() {
		return Collections.unmodifiableCollection(sequences.values());
	}

	public Set<UUID> getSequenceUUIDs() {
		return Collections.unmodifiableSet(sequences.keySet());
	}

	public Collection<GSTrack> getTracks() {
		return Collections.unmodifiableCollection(tracks.values());
	}
	
	public Set<UUID> getTrackUUIDs() {
		return Collections.unmodifiableSet(tracks.keySet());
	}
	
	public boolean isComplete() {
		for (GSTrack track : getTracks()) {
			if (!track.isComplete())
				return false;
		}
		
		return true;
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

	private void dispatchSequenceAdded(GSSequence sequence) {
		for (GSICompositionListener listener : getListeners())
			listener.sequenceAdded(sequence);
	}

	private void dispatchSequenceRemoved(GSSequence sequence) {
		for (GSICompositionListener listener : getListeners())
			listener.sequenceRemoved(sequence);
	}
	
	private void dispatchTrackAdded(GSTrack track) {
		for (GSICompositionListener listener : getListeners())
			listener.trackAdded(track);
	}

	private void dispatchTrackRemoved(GSTrack track) {
		for (GSICompositionListener listener : getListeners())
			listener.trackRemoved(track);
	}

	public static GSComposition readComposition(PacketByteBuf buf) throws IOException {
		// Skip reserved byte
		buf.readByte();
		
		UUID compositionUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSComposition composition = new GSComposition(compositionUUID, name);

		int sequenceCount = buf.readInt();
		while (sequenceCount-- != 0) {
			GSSequence sequence = GSSequence.read(buf);
			if (composition.hasSequenceUUID(sequence.getSequenceUUID()))
				throw new IOException("Duplicate sequence UUID");
			composition.addSequenceInternal(sequence);
		}

		int trackCount = buf.readInt();
		while (trackCount-- != 0) {
			GSTrack track = GSTrack.read(buf);
			if (composition.hasTrackUUID(track.getTrackUUID()))
				throw new IOException("Duplicate track UUID");
			composition.addTrackInternal(track);
		}
		
		if (!composition.isComplete())
			throw new IOException("Composition is not complete!");
		
		return composition;
	}

	public static void write(PacketByteBuf buf, GSComposition composition) throws IOException {
		// Reserved byte for future use
		buf.writeByte(0x00);

		buf.writeUuid(composition.getCompositionUUID());
		buf.writeString(composition.getName());

		Collection<GSSequence> sequences = composition.getSequences();
		buf.writeInt(sequences.size());
		for (GSSequence sequence : sequences)
			GSSequence.write(buf, sequence);
		
		Collection<GSTrack> tracks = composition.getTracks();
		buf.writeInt(tracks.size());
		for (GSTrack track : tracks)
			GSTrack.write(buf, track);
	}
}
