package com.g4mesoft.captureplayback.timeline;

public interface GSITimelineListener {

	default public void timelineNameChanged(String oldName) {
	}
	
	default public void trackAdded(GSTrack track) {
	}

	default public void trackRemoved(GSTrack track) {
	}
	
	default public void trackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
	}

	default public void trackDisabledChanged(GSTrack track, boolean oldDisabled) {
	}

	default public void entryAdded(GSTrackEntry entry) {
	}

	default public void entryRemoved(GSTrackEntry entry) {
	}

	default public void entryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
	}

	default public void entryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
	}
}
