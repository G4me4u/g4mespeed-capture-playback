package com.g4mesoft.captureplayback.playlist.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSEntryMovedDelta extends GSEntryDelta {

	private UUID newPrevUUID;
	private UUID oldPrevUUID;

	public GSEntryMovedDelta() {
	}

	public GSEntryMovedDelta(UUID entryUUID, UUID newPrevUUID, UUID oldPrevUUID) {
		super(entryUUID);
		
		this.newPrevUUID = newPrevUUID;
		this.oldPrevUUID = oldPrevUUID;
	}
	
	private void moveEntry(UUID newPrevUUID, UUID oldPrevUUID, GSPlaylist playlist) throws GSDeltaException {
		checkPreviousEntry(getEntry(playlist), oldPrevUUID);
		playlist.moveEntryAfter(entryUUID, newPrevUUID);
	}
	
	@Override
	public void unapply(GSPlaylist playlist) throws GSDeltaException {
		moveEntry(oldPrevUUID, newPrevUUID, playlist);
	}

	@Override
	public void apply(GSPlaylist playlist) throws GSDeltaException {
		moveEntry(newPrevUUID, oldPrevUUID, playlist);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newPrevUUID = buf.readBoolean() ? buf.readUUID() : null;
		oldPrevUUID = buf.readBoolean() ? buf.readUUID() : null;
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeBoolean(newPrevUUID != null);
		if (newPrevUUID != null)
			buf.writeUUID(newPrevUUID);
		buf.writeBoolean(oldPrevUUID != null);
		if (oldPrevUUID != null)
			buf.writeUUID(oldPrevUUID);
	}
}
