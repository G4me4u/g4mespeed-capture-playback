package com.g4mesoft.captureplayback.composition;

import com.g4mesoft.captureplayback.sequence.GSSequence;

public interface GSICompositionListener {

	default public void compositionNameChanged(String oldName) {
	}

	default public void sequenceAdded(GSSequence sequence) {
	}

	default public void sequenceRemoved(GSSequence sequence) {
	}
	
	default public void trackAdded(GSTrack track) {
	}

	default public void trackRemoved(GSTrack track) {
	}

	default public void trackNameChanged(GSTrack track, String oldName) {
	}

	default public void trackColorChanged(GSTrack gsTrack, int oldColor) {
	}
	
	default public void entryAdded(GSTrackEntry entry) {
	}

	default public void entryRemoved(GSTrackEntry entry) {
	}

	default public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
	}
}
