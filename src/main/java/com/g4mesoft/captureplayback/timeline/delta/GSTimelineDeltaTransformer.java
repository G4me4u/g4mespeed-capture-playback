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
	private boolean disabled;
	
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
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Override
	public void timelineNameChanged(String oldName) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSTimelineNameDelta(timeline.getName(), oldName));
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSTrackAddedDelta(track));
	}

	@Override
	public void trackRemoved(GSTrack track) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSTrackRemovedDelta(track));
	}
	
	@Override
	public void trackInfoChanged(GSTrack track, GSTrackInfo oldInfo) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSTrackInfoDelta(track.getTrackUUID(), track.getInfo(), oldInfo));
	}

	@Override
	public void trackDisabledChanged(GSTrack track, boolean oldDisabled) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSTrackDisabledDelta(track.getTrackUUID(), track.isDisabled(), oldDisabled));
	}

	@Override
	public void entryAdded(GSTrackEntry entry) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSEntryAddedDelta(entry));
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		if (!disabled)
			dispatchTimelineDeltaEvent(new GSEntryRemovedDelta(entry));
	}

	@Override
	public void entryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
		if (!disabled) {
			dispatchTimelineDeltaEvent(new GSEntryTimeDelta(entry.getOwnerTrack().getTrackUUID(),
					entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime(), oldStart, oldEnd));
		}
	}

	@Override
	public void entryTypeChanged(GSTrackEntry entry, GSETrackEntryType oldType) {
		if (!disabled) {
			dispatchTimelineDeltaEvent(new GSEntryTypeDelta(entry.getOwnerTrack().getTrackUUID(),
					entry.getEntryUUID(), entry.getType(), oldType));
		}
	}
	
	private void dispatchTimelineDeltaEvent(GSITimelineDelta delta) {
		for (GSITimelineDeltaListener listener : listeners)
			listener.onTimelineDelta(delta);
	}
}
