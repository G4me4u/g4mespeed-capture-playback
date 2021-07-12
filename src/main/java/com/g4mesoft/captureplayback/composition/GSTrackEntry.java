package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.network.PacketByteBuf;

public class GSTrackEntry {

	private final UUID entryUUID;
	private long offset;
	
	private GSTrack parent;
	
	GSTrackEntry(GSTrackEntry other) {
		this(other.getEntryUUID(), other.getOffset());
	}
	
	GSTrackEntry(UUID entryUUID, long offset) {
		if (offset < 0L)
			throw new IllegalArgumentException("offset must be non-negative");
		
		this.entryUUID = entryUUID;
		this.offset = offset;
		
		parent = null;
	}
	
	public GSTrack getParent() {
		return parent;
	}

	void onAdded(GSTrack parent) {
		if (this.parent != null)
			throw new IllegalStateException("Entry already has a parent");
		
		this.parent = parent;
	}

	void onRemoved(GSTrack parent) {
		if (this.parent != parent)
			throw new IllegalStateException("Entry does not have specified parent");
		
		this.parent = null;
	}
	
	public void set(GSTrackEntry other) {
		setOffset(other.getOffset());
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
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
		long offset = buf.readLong();

		return new GSTrackEntry(entryUUID, offset);
	}

	public static void write(PacketByteBuf buf, GSTrackEntry entry) throws IOException {
		buf.writeUuid(entry.getEntryUUID());
		buf.writeLong(entry.getOffset());
	}
}
