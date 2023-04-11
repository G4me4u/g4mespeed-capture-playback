package com.g4mesoft.captureplayback.playlist.delta;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaTransformer;
import com.g4mesoft.captureplayback.playlist.GSEPlaylistEntryType;
import com.g4mesoft.captureplayback.playlist.GSETriggerType;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistData;
import com.g4mesoft.captureplayback.playlist.GSIPlaylistListener;
import com.g4mesoft.captureplayback.playlist.GSPlaylist;
import com.g4mesoft.captureplayback.playlist.GSPlaylistEntry;

public class GSPlaylistDeltaTransformer extends GSDeltaTransformer<GSPlaylist> implements GSIPlaylistListener {

	@Override
	public void install(GSPlaylist model) {
		super.install(model);
		
		model.addPlaylistListener(this);
	}
	
	public void uninstall(GSPlaylist model) {
		super.uninstall(model);
		
		model.removePlaylistListener(this);
	}
	
	@Override
	public void playlistNameChanged(String oldName) {
		dispatchDeltaEvent(new GSPlaylistNameDelta(model.getName(), oldName));
	}
	
	@Override
	public void triggerChanged(GSETriggerType oldType, GSIPlaylistData oldData) {
		dispatchDeltaEvent(new GSTriggerDelta(model.getTrigger(), oldType, oldData));
	}

	@Override
	public void entryAdded(GSPlaylistEntry entry, UUID prevUUID) {
		dispatchDeltaEvent(new GSEntryAddedDelta(entry, prevUUID));
	}

	@Override
	public void entryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
		dispatchDeltaEvent(new GSEntryRemovedDelta(entry, oldPrevUUID));
	}

	@Override
	public void entryMoved(GSPlaylistEntry entry, UUID newPrevUUID, UUID oldPrevUUID) {
		dispatchDeltaEvent(new GSEntryMovedDelta(entry.getEntryUUID(), newPrevUUID, oldPrevUUID));
	}
	
	@Override
	public void entryChanged(GSPlaylistEntry entry, GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
		dispatchDeltaEvent(new GSEntryChangedDelta(entry, oldType, oldData));
	}
}
