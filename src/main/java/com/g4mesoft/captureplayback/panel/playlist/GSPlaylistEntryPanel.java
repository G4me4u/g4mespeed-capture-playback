package com.g4mesoft.captureplayback.panel.playlist;

import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;

public class GSPlaylistEntryPanel extends GSAbstractPlaylistEntryPanel<GSEPlaylistEntryType> {

	public GSPlaylistEntryPanel(GSPlaylistEntry entry) {
		super(entry, GSEPlaylistEntryType.TYPES);
	}

	
}
