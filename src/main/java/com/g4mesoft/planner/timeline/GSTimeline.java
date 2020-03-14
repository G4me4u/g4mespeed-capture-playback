package com.g4mesoft.planner.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSTimeline {

	public static final int PROPERTY_NAME = 0;
	
	private String name;
	
	private final List<GSTrack> tracks;
	private final List<GSITimelineListener> listeners;
	
	public GSTimeline() {
		tracks = new ArrayList<GSTrack>();
		listeners = new ArrayList<GSITimelineListener>();
	}
	
	public void addTrack(GSTrackInfo info) {
		GSTrack track = new GSTrack(info, this);
		tracks.add(track);
		
		dispatchTrackAdded(track);
	}

	public void addTimelineListener(GSITimelineListener listener) {
		listeners.add(listener);
	}

	public void removeTimelineListener(GSITimelineListener listener) {
		listeners.remove(listener);
	}
	
	void onTrackPropertyChanged(GSTrack track, int property) {
		dispatchTrackPropertyChanged(track, property);
	}

	void onEntryAdded(GSTrack track, GSTrackEntry entry) {
		dispatchEntryAdded(track, entry);
	}
	
	void onEntryRemoved(GSTrack track, GSTrackEntry entry) {
		dispatchEntryRemoved(track, entry);
	}
	
	void onEntryPropertyChanged(GSTrack track, GSTrackEntry entry, int property) {
		dispatchEntryPropertyChanged(track, entry, property);
	}
	
	private void dispatchTimelinePropertyChanged(int property) {
		for (GSITimelineListener listener : listeners)
			listener.timelinePropertyChanged(property);
	}

	private void dispatchTrackAdded(GSTrack track) {
		for (GSITimelineListener listener : listeners)
			listener.trackAdded(track);
	}

	private void dispatchTrackPropertyChanged(GSTrack track, int property) {
		for (GSITimelineListener listener : listeners)
			listener.trackPropertyChanged(track, property);
	}

	private void dispatchEntryAdded(GSTrack track, GSTrackEntry entry) {
		for (GSITimelineListener listener : listeners)
			listener.entryAdded(track, entry);
	}

	private void dispatchEntryRemoved(GSTrack track, GSTrackEntry entry) {
		for (GSITimelineListener listener : listeners)
			listener.entryRemoved(track, entry);
	}

	private void dispatchEntryPropertyChanged(GSTrack track, GSTrackEntry entry, int property) {
		for (GSITimelineListener listener : listeners)
			listener.entryPropertyChanged(track, entry, property);
	}
	
	public void setName(String name) {
		this.name = name;
		
		dispatchTimelinePropertyChanged(PROPERTY_NAME);
	}
	
	public String getName() {
		return name;
	}
	
	public List<GSTrack> getTracks() {
		return Collections.unmodifiableList(tracks);
	}
}
