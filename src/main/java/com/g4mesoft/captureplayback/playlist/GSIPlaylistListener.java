package com.g4mesoft.captureplayback.playlist;

import java.util.UUID;

public interface GSIPlaylistListener {

	default public void playlistNameChanged(String oldName) {
	}

	default public void triggerChanged(GSETriggerType oldType, GSIPlaylistData oldData) {
	}

	default public void entryAdded(GSPlaylistEntry entry, UUID prevUUID) {
	}

	default public void entryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
	}

	default public void entryMoved(GSPlaylistEntry entry, UUID newPrevUUID, UUID oldPrevUUID) {
	}
	
	default public void entryChanged(GSPlaylistEntry entry, GSEPlaylistEntryType oldType, GSIPlaylistData oldData) {
	}
}
