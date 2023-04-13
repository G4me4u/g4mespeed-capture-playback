package com.g4mesoft.captureplayback.playlist.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistDataRegistry;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSEntryRemovedDelta extends GSEntryDelta {

	private UUID prevUUID;
	private GSEPlaylistEntryType type;
	private GSIPlaylistData data;

	public GSEntryRemovedDelta() {
	}

	public GSEntryRemovedDelta(GSPlaylistEntry entry, UUID prevUUID) {
		this(entry.getEntryUUID(), prevUUID, entry.getType(), entry.getData());
	}
	
	public GSEntryRemovedDelta(UUID entryUUID, UUID prevUUID, GSEPlaylistEntryType type, GSIPlaylistData data) {
		super(entryUUID);
		
		this.prevUUID = prevUUID;
		this.type = type;
		this.data = data;
	}
	
	@Override
	public void unapply(GSPlaylist playlist) throws GSDeltaException {
		addEntry(playlist, prevUUID, type, data);
	}

	@Override
	public void apply(GSPlaylist playlist) throws GSDeltaException {
		removeEntry(playlist, prevUUID, type, data);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		type = GSEPlaylistEntryType.fromIndex(buf.readUnsignedByte());
		if (type == null)
			throw new IOException("Unknown entry type");
		data = GSPlaylistDataRegistry.readData(buf);
		
		prevUUID = buf.readBoolean() ? buf.readUUID() : null;
	}
	
	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);
	
		buf.writeUnsignedByte((short)type.getIndex());
		GSPlaylistDataRegistry.writeData(buf, data);
		
		buf.writeBoolean(prevUUID != null);
		if (prevUUID != null)
			buf.writeUUID(prevUUID);
	}
}
