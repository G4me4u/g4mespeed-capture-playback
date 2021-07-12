package com.g4mesoft.captureplayback.composition;

import java.util.UUID;

public interface GSICompositionListener {

	default public void compositionNameChanged(String oldName) {
	}

	default public void groupAdded(GSTrackGroup group) {
	}

	default public void groupRemoved(GSTrackGroup group) {
	}
	
	default public void groupNameChanged(GSTrackGroup group, String oldName) {
	}
	
	default public void trackAdded(GSTrack track) {
	}

	default public void trackRemoved(GSTrack track) {
	}

	default public void trackNameChanged(GSTrack track, String oldName) {
	}

	default public void trackColorChanged(GSTrack track, int oldColor) {
	}

	default public void trackGroupChanged(GSTrack track, UUID oldGroupUUID) {
	}
	
	default public void entryAdded(GSTrackEntry entry) {
	}

	default public void entryRemoved(GSTrackEntry entry) {
	}

	default public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
	}
}
