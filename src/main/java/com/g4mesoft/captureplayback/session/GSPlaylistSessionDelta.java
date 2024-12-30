package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlaylistSessionDelta implements GSIDelta<GSSession> {
	
	private GSSessionFieldType<?> type;
	private GSIDelta<GSPlaylist> delta;
	
	public GSPlaylistSessionDelta() {
	}
	
	public GSPlaylistSessionDelta(GSSessionFieldType<GSPlaylist> type, GSIDelta<GSPlaylist> delta) {
		this.type = type;
		this.delta = delta;
	}
	
	@Override
	public void apply(GSSession session) throws GSDeltaException {
		GSSessionField<?> field = session.getField(type);
		if (!(field instanceof GSPlaylistSessionField))
			throw new GSDeltaException("Field '" + type.getName() + "' is not a playlist.");
		((GSPlaylistSessionField)field).applyDelta(delta);
	}
	
	@Override
	public void unapply(GSSession session) throws GSDeltaException {
		throw new GSDeltaException("Unapply unsupported.");
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		type = GSSession.readFieldType(buf);
		delta = GSDeltaRegistries.PLAYLIST_DELTA_REGISTRY.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSSession.writeFieldType(buf, type);
		GSDeltaRegistries.PLAYLIST_DELTA_REGISTRY.write(buf, delta);
	}
}
