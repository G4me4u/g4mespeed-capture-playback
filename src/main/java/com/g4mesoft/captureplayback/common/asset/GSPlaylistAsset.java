package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.playlist.GSIPlaylistListener;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlaylistAsset extends GSAbstractAsset implements GSIPlaylistListener {

	private final GSPlaylist playlist;
	
	public GSPlaylistAsset(GSAssetInfo info) {
		this(new GSPlaylist(info.getAssetUUID(), info.getAssetName()));
	}
	
	public GSPlaylistAsset(GSPlaylist playlist) {
		super(GSEAssetType.PLAYLIST);
		
		this.playlist = playlist;
	}
	
	@Override
	protected void duplicateFrom(GSAbstractAsset other) {
		if (!(other instanceof GSPlaylistAsset))
			throw new IllegalArgumentException("Expected playlist asset");
		playlist.duplicateFrom(((GSPlaylistAsset)other).getPlaylist());
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();
		
		playlist.addPlaylistListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();
		
		playlist.removePlaylistListener(this);
	}
	
	public GSPlaylist getPlaylist() {
		return playlist;
	}
	
	@Override
	public UUID getUUID() {
		return playlist.getPlaylistUUID();
	}
	
	@Override
	public String getName() {
		return playlist.getName();
	}
	
	@Override
	public GSIPlaybackStream getPlaybackStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public GSICaptureStream getCaptureStream() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Iterator<UUID> getDerivedIterator() {
		return Collections.emptyIterator();
	}

	@Override
	public GSAbstractAsset getDerivedAsset(UUID assetUUID) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void playlistNameChanged(String oldName) {
		dispatchNameChanged(playlist.getName());
	}

	public static GSPlaylistAsset read(GSDecodeBuffer buf) throws IOException {
		return new GSPlaylistAsset(GSPlaylist.read(buf));
	}
	
	public static void write(GSEncodeBuffer buf, GSPlaylistAsset asset) throws IOException {
		GSPlaylist.write(buf, asset.getPlaylist());
	}
}
