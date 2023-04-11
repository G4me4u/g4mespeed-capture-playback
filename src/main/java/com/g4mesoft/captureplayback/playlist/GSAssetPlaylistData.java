package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSAssetPlaylistData implements GSIPlaylistData {

	private final UUID assetUUID;
	
	public GSAssetPlaylistData(UUID assetUUID) {
		if (assetUUID == null)
			throw new IllegalArgumentException("assetUUID is null!");
		this.assetUUID = assetUUID;
	}
	
	public UUID getAssetUUID() {
		return assetUUID;
	}

	@Override
	public int hashCode() {
		return assetUUID.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSAssetPlaylistData) {
			GSAssetPlaylistData other = (GSAssetPlaylistData)obj;
			return assetUUID.equals(other.assetUUID);
		}
		return false;
	}
	
	public static GSAssetPlaylistData read(GSDecodeBuffer buf) throws IOException {
		UUID assetUUID = buf.readUUID();
		return new GSAssetPlaylistData(assetUUID);
	}

	public static void write(GSEncodeBuffer buf, GSAssetPlaylistData data) throws IOException {
		buf.writeUUID(data.getAssetUUID());
	}
}
