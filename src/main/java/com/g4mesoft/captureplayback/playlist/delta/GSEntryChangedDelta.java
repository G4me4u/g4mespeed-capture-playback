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

public class GSEntryChangedDelta extends GSEntryDelta {

	private GSEPlaylistEntryType newType;
	private GSIPlaylistData newData;
	private GSEPlaylistEntryType oldType;
	private GSIPlaylistData oldData;

	public GSEntryChangedDelta() {
	}

	public GSEntryChangedDelta(GSPlaylistEntry entry, GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
		this(entry.getEntryUUID(), entry.getType(), entry.getData(), oldType, oldData);
	}
	
	public GSEntryChangedDelta(UUID entryUUID, GSEPlaylistEntryType newType, GSIPlaylistData newData,
			GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
		
		super(entryUUID);
		
		this.newType = newType;
		this.newData = newData;
		this.oldType = oldType;
		this.oldData = oldData;
	}
	
	@Override
	public void unapply(GSPlaylist playlist) throws GSDeltaException {
		GSPlaylistEntry entry = getEntry(playlist);
		checkEntry(entry, newType, newData);
		try {
			entry.set(oldType, oldData);
		} catch (Throwable t) {
			// In case data does not match type.
			throw new GSDeltaException(t);
		}
	}

	@Override
	public void apply(GSPlaylist playlist) throws GSDeltaException {
		GSPlaylistEntry entry = getEntry(playlist);
		checkEntry(entry, oldType, oldData);
		try {
			entry.set(newType, newData);
		} catch (Throwable t) {
			throw new GSDeltaException(t);
		}
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		newType = GSEPlaylistEntryType.fromIndex(buf.readUnsignedByte());
		if (newType == null)
			throw new IOException("Unknown entry type");
		newData = GSPlaylistDataRegistry.readData(buf);
		oldType = GSEPlaylistEntryType.fromIndex(buf.readUnsignedByte());
		if (oldType == null)
			throw new IOException("Unknown entry type");
		oldData = GSPlaylistDataRegistry.readData(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUnsignedByte((short)newType.getIndex());
		GSPlaylistDataRegistry.writeData(buf, newData);
		buf.writeUnsignedByte((short)oldType.getIndex());
		GSPlaylistDataRegistry.writeData(buf, oldData);
	}
}
