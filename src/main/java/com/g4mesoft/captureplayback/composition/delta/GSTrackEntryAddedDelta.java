package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;

import net.minecraft.network.PacketByteBuf;

public class GSTrackEntryAddedDelta extends GSTrackEntryDelta {

	private long offset;
	
	public GSTrackEntryAddedDelta() {
	}

	public GSTrackEntryAddedDelta(GSTrackEntry entry) {
		this(entry.getParent().getTrackUUID(), entry.getEntryUUID(), entry.getOffset());
	}
	
	public GSTrackEntryAddedDelta(UUID channelUUID, UUID entryUUID, long offset) {
		super(channelUUID, entryUUID);
		
		this.offset = offset;
	}
	
	@Override
	public void unapplyDelta(GSComposition composition) throws GSCompositionDeltaException {
		removeEntry(composition, offset);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSCompositionDeltaException {
		addEntry(composition, offset);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		offset = buf.readLong();
		
		if (offset < 0L)
			throw new IOException("Invalid offset");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeLong(offset);
	}
}
