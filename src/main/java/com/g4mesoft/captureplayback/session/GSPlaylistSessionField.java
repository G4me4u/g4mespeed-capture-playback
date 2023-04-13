package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.delta.GSPlaylistDeltaTransformer;

public class GSPlaylistSessionField extends GSAssetSessionField<GSPlaylist> {

	public GSPlaylistSessionField(GSSessionFieldType<GSPlaylist> type) {
		super(type, new GSPlaylistDeltaTransformer());
	}
	
	@Override
	public void onDelta(GSIDelta<GSPlaylist> delta) {
		if (session != null)
			session.dispatchSessionDelta(new GSPlaylistSessionDelta(type, delta));
	}
}
