package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackEntryRemovedDelta extends GSTrackEntryDelta {

	private long offset;
	
	public GSTrackEntryRemovedDelta() {
	}

	public GSTrackEntryRemovedDelta(GSTrackEntry entry) {
		this(entry.getParent().getTrackUUID(), entry.getEntryUUID(), entry.getOffset());
	}
	
	public GSTrackEntryRemovedDelta(UUID channelUUID, UUID entryUUID, long offset) {
		super(channelUUID, entryUUID);
		
		this.offset = offset;
	}

	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		addEntry(composition, offset);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		removeEntry(composition, offset);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		offset = buf.readLong();

		if (offset < 0L)
			throw new IOException("Invalid offset");
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeLong(offset);
	}
}
