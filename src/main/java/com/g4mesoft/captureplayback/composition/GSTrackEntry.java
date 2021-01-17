package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.network.PacketByteBuf;

public class GSTrackEntry {

	private final UUID entryUUID;
	private final UUID sequenceUUID;
	private long offset;
	
	private GSTrack parent;
	
	public GSTrackEntry(UUID entryUUID, UUID sequenceUUID, long offset) {
		if (offset < 0L)
			throw new IllegalArgumentException("offset must be non-negative");
		
		this.entryUUID = entryUUID;
		this.sequenceUUID = sequenceUUID;
		this.offset = offset;
		
		parent = null;
	}
	
	public GSTrack getParent() {
		return parent;
	}

	void setParent(GSTrack parent) {
		if (this.parent != null)
			throw new IllegalStateException("Entry already has a parent");
		this.parent = parent;
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}

	public UUID getSequenceUUID() {
		return sequenceUUID;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public void setOffset(long offset) {
		if (offset < 0L)
			throw new IllegalArgumentException("offset must be non-negative");
		
		if (offset != this.offset) {
			long oldOffset = this.offset;
			this.offset = offset;
			
			dispatchOffsetChanged(this, oldOffset);
		}
	}

	private void dispatchOffsetChanged(GSTrackEntry entry, long oldOffset) {
		if (parent != null && parent.getParent() != null) {
			for (GSICompositionListener listener : parent.getParent().getListeners())
				listener.entryOffsetChanged(entry, oldOffset);
		}
	}
	
	public static GSTrackEntry read(PacketByteBuf buf) throws IOException {
		UUID entryUUID = buf.readUuid();
		UUID sequenceUUID = buf.readUuid();
		long offset = buf.readLong();

		return new GSTrackEntry(entryUUID, sequenceUUID, offset);
	}

	public static void write(PacketByteBuf buf, GSTrackEntry entry) throws IOException {
		buf.writeUuid(entry.getEntryUUID());
		buf.writeUuid(entry.getSequenceUUID());
		buf.writeLong(entry.getOffset());
	}
}
