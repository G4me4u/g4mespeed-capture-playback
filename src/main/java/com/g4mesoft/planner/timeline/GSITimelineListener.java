package com.g4mesoft.planner.timeline;

public interface GSITimelineListener {

	public void timelinePropertyChanged(int property);
	
	public void trackAdded(GSTrack track);

	public void trackPropertyChanged(GSTrack track, int property);

	public void entryAdded(GSTrack track, GSTrackEntry entry);

	public void entryRemoved(GSTrack track, GSTrackEntry entry);

	public void entryPropertyChanged(GSTrack track, GSTrackEntry entry, int property);
	
}
