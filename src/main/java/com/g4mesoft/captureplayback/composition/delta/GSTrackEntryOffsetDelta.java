package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;

import net.minecraft.network.PacketByteBuf;

public class GSTrackEntryOffsetDelta extends GSTrackEntryDelta {

	private long newOffset;
	private long oldOffset;

	public GSTrackEntryOffsetDelta() {
	}
	
	public GSTrackEntryOffsetDelta(UUID trackUUID, UUID entryUUID, long newOffset, long oldOffset) {
		super(trackUUID, entryUUID);
		
		this.newOffset = newOffset;
		this.oldOffset = oldOffset;
	}

	private void setEntryOffset(GSComposition composition, long newOffset, long oldOffset) throws GSCompositionDeltaException {
		GSTrackEntry entry = getEntry(composition);
		checkEntryOffset(entry, oldOffset);
		entry.setOffset(newOffset);
	}
	
	@Override
	public void unapplyDelta(GSComposition composition) throws GSCompositionDeltaException {
		setEntryOffset(composition, oldOffset, newOffset);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSCompositionDeltaException {
		setEntryOffset(composition, newOffset, oldOffset);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newOffset = buf.readLong();
		oldOffset = buf.readLong();
	
		if (newOffset < 0L || oldOffset < 0L)
			throw new IOException("Invalid offsets");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeLong(newOffset);
		buf.writeLong(oldOffset);
	}
}
