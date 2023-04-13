package com.g4mesoft.captureplayback.playlist.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlaylistNameDelta implements GSIDelta<GSPlaylist> {

	private String newName;
	private String oldName;

	public GSPlaylistNameDelta() {
	}
	
	public GSPlaylistNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSPlaylist playlist) throws GSDeltaException {
		if (!Objects.equals(oldName, playlist.getName()))
			throw new GSDeltaException("Playlist does not have the expected name");
		playlist.setName(newName);
	}
	
	@Override
	public void unapply(GSPlaylist playlist) throws GSDeltaException {
		setName(oldName, newName, playlist);
	}

	@Override
	public void apply(GSPlaylist playlist) throws GSDeltaException {
		setName(newName, oldName, playlist);
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		newName = buf.readString();
		oldName = buf.readString();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
