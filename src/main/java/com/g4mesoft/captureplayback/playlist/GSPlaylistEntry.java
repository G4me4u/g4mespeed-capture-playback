package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlaylistEntry extends GSAbstractPlaylistEntry<GSEPlaylistEntryType> {

	private final UUID entryUUID;
	
	public GSPlaylistEntry(GSPlaylistEntry other) {
		this(other.getEntryUUID(), other.getType(), other.getData());
	}
	
	public GSPlaylistEntry(UUID entryUUID, GSEPlaylistEntryType type, GSIPlaylistData data) {
		super(type, data);
		
		if (entryUUID == null)
			throw new IllegalArgumentException("entryUUID is null!");
		
		// Note: onAdded has not been invoked, so we do not
		// dispatch any events (where entry UUID is required).
		this.entryUUID = entryUUID;
	}
	
	@Override
	protected void dispatchDataChanged(GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
		GSPlaylist parent = getParent();
		if (parent != null) {
			for (GSIPlaylistListener listener : parent.getListeners())
				listener.entryChanged(this, oldType, oldData);
		}
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}
	
	public static GSPlaylistEntry read(GSDecodeBuffer buf) throws IOException {
		UUID entryUUID = buf.readUUID();
		GSEPlaylistEntryType type = GSEPlaylistEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Unknown entry type");
		GSIPlaylistData data = GSPlaylistDataRegistry.readData(buf);
		return new GSPlaylistEntry(entryUUID, type, data);
	}

	public static void write(GSEncodeBuffer buf, GSPlaylistEntry entry) throws IOException {
		buf.writeUUID(entry.getEntryUUID());
		buf.writeInt(entry.getType().getIndex());
		GSPlaylistDataRegistry.writeData(buf, entry.getData());
	}
}
