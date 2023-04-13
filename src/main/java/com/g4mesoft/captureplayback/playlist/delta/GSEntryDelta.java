package com.g4mesoft.captureplayback.playlist.delta;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public abstract class GSEntryDelta implements GSIDelta<GSPlaylist> {

	protected UUID entryUUID;

	protected GSEntryDelta() {
	}
	
	protected GSEntryDelta(UUID entryUUID) {
		this.entryUUID = entryUUID;
	}
	
	protected GSPlaylistEntry getEntry(GSPlaylist playlist) throws GSDeltaException {
		GSPlaylistEntry entry = playlist.getEntry(entryUUID);
		if (entry == null)
			throw new GSDeltaException("Expected entry does not exist");
		return entry;
	}
	
	protected void checkPreviousEntry(GSPlaylistEntry entry, UUID expectedPrevUUID) throws GSDeltaException {
		GSPlaylistEntry prevEntry = entry.getParent().getPreviousEntry(entryUUID);
		UUID prevUUID = (prevEntry == null) ? null : prevEntry.getEntryUUID();
		if (!Objects.equals(prevUUID, expectedPrevUUID))
			throw new GSDeltaException("Entry does not have the expected previous entry");
	}

	protected void checkEntry(GSPlaylistEntry entry, GSEPlaylistEntryType type, GSIPlaylistData data) throws GSDeltaException {
		if (entry.getType() != type)
			throw new GSDeltaException("Entry does not have the expected type: " + type);
		if (!entry.getData().equals(data))
			throw new GSDeltaException("Entry does not have the expected data");
	}
	
	protected void removeEntry(GSPlaylist playlist, UUID prevUUID, GSEPlaylistEntryType expectedType,
			GSIPlaylistData expectedData) throws GSDeltaException {
		
		GSPlaylistEntry entry = getEntry(playlist);
		checkPreviousEntry(entry, prevUUID);
		checkEntry(entry, expectedType, expectedData);
		playlist.removeEntry(entryUUID);
	}
	
	protected GSPlaylistEntry addEntry(GSPlaylist playlist, UUID prevUUID, GSEPlaylistEntryType type,
			GSIPlaylistData data) throws GSDeltaException {
		
		if (playlist.hasEntryUUID(entryUUID))
			throw new GSDeltaException("Entry already exists");
		
		try {
			GSPlaylistEntry entry = playlist.addEntry(entryUUID, type, data);
			playlist.moveEntryAfter(entryUUID, prevUUID);
			return entry;
		} catch (Throwable t) {
			playlist.removeEntry(entryUUID);
			throw new GSDeltaException("Failed to add entry", t);
		}
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		entryUUID = buf.readUUID();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUUID(entryUUID);
	}
}
