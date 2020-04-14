package com.g4mesoft.captureplayback.timeline.delta;

import java.util.ArrayList;
import java.util.List;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

public class GSTimelineDeltaTransformer implements GSITimelineListener {

	private final List<GSITimelineDeltaListener> listeners;
	
	private GSTimeline timeline;
	
	public GSTimelineDeltaTransformer() {
		listeners = new ArrayList<GSITimelineDeltaListener>();
	
		timeline = null;
	}
	
	public void addDeltaListener(GSITimelineDeltaListener listener) {
		listeners.add(listener);
	}

	public void removeDeltaListener(GSITimelineDeltaListener listener) {
		listeners.remove(listener);
	}
	
	public void install(GSTimeline timeline) {
		if (this.timeline != null)
			throw new IllegalStateException("Already installed");
		
		this.timeline = timeline;
	
		timeline.addTimelineListener(this);
	}
	
	public void uninstall(GSTimeline timeline) {
		if (this.timeline == null)
			throw new IllegalStateException("Not installed");
		if (this.timeline != timeline)
			throw new IllegalStateException("Timeline is not the one that is installed");
		
		this.timeline.removeTimelineListener(this);
		
		this.timeline = null;
	}
	
	@Override
	public void timelineNameChanged(String oldName) {
		dispatchTimelineDeltaEvent(new GSTimelineNameDelta(timeline.getName(), oldName));
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		dispatchTimelineDeltaEvent(new GSTrackAddedDelta(track.getTrackUUID(), track.getInfo()));
	}

	@Override
	public void trackRemoved(GSTrack track) {
		dispatchTimelineDeltaEvent(new GSTrackRemovedDelta(track.getTrackUUID(), track.getInfo()));
	}
	
	@Override
	public void trackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
		dispatchTimelineDeltaEvent(new GSTrackInfoDelta(track.getTrackUUID(), track.getInfo(), oldInfo));
	}

	@Override
	public void trackDisabledChanged(GSTrack track, boolean oldDisabled) {
		dispatchTimelineDeltaEvent(new GSTrackDisabledDelta(track.getTrackUUID(), track.isDisabled(), oldDisabled));
	}

	@Override
	public void entryAdded(GSTrackEntry entry) {
		dispatchTimelineDeltaEvent(new GSEntryAddedDelta(entry.getTrack().getTrackUUID(), 
				entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime()));
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		dispatchTimelineDeltaEvent(new GSEntryRemovedDelta(entry.getTrack().getTrackUUID(), 
				entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime()));
	}

	@Override
	public void entryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
		dispatchTimelineDeltaEvent(new GSEntryTimeDelta(entry.getTrack().getTrackUUID(),
				entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime(), oldStart, oldEnd));
	}

	@Override
	public void entryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
		dispatchTimelineDeltaEvent(new GSEntryTypeDelta(entry.getTrack().getTrackUUID(),
				entry.getEntryUUID(), entry.getType(), oldType));
	}
	
	private void dispatchTimelineDeltaEvent(GSITimelineDelta delta) {
		for (GSITimelineDeltaListener listener : listeners)
			listener.onTimelineDelta(delta);
	}
}
