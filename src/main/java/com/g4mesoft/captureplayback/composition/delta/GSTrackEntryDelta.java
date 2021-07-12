package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;

import net.minecraft.network.PacketByteBuf;

public abstract class GSTrackEntryDelta extends GSTrackDelta {

	protected UUID entryUUID;

	protected GSTrackEntryDelta() {
	}
	
	protected GSTrackEntryDelta(UUID trackUUID, UUID entryUUID) {
		super(trackUUID);
		
		this.entryUUID = entryUUID;
	}

	protected GSTrackEntry getEntry(GSComposition composition) throws GSCompositionDeltaException {
		return getEntry(getTrack(composition));
	}
	
	protected GSTrackEntry getEntry(GSTrack track) throws GSCompositionDeltaException {
		GSTrackEntry entry = track.getEntry(entryUUID);
		if (entry == null)
			throw new GSCompositionDeltaException("Expected entry does not exist");
		
		return entry;
	}
	
	protected void checkEntryOffset(GSTrackEntry entry, long expectedOffset) throws GSCompositionDeltaException {
		if (entry.getOffset() != expectedOffset)
			throw new GSCompositionDeltaException("Entry does not have the expected offset");
	}
	
	protected void removeEntry(GSComposition composition, long offset) throws GSCompositionDeltaException {
		GSTrackEntry entry = getEntry(composition);
		checkEntryOffset(entry, offset);
		entry.getParent().removeEntry(entryUUID);
	}
	
	protected GSTrackEntry addEntry(GSComposition composition, long offset) throws GSCompositionDeltaException {
		GSTrack track = getTrack(composition);
		if (track.hasEntryUUID(entryUUID))
			throw new GSCompositionDeltaException("Entry already exists");
		return track.addEntry(entryUUID, offset);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		entryUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
		
		buf.writeUuid(entryUUID);
	}
}
