package com.g4mesoft.captureplayback.timeline.delta;

import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

public class GSTrackRemovedDelta extends GSTrackAddedDelta {

	public GSTrackRemovedDelta(UUID trackUUID, GSTrackInfo info) {
		super(trackUUID, info);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		super.applyDelta(timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		super.unapplyDelta(timeline);
	}
}
