package com.g4mesoft.captureplayback.composition;

public interface GSICompositionListener {

	default public void sequenceNameChanged(String oldName) {
	}

	default public void trackAdded(GSTrack track) {
	}

	default public void trackRemoved(GSTrack track) {
	}

	default public void trackNameChanged(GSTrack track, String oldName) {
	}
	
	default public void entryAdded(GSTrackEntry entry) {
	}

	default public void entryRemoved(GSTrackEntry entry) {
	}

	default public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
	}
}
