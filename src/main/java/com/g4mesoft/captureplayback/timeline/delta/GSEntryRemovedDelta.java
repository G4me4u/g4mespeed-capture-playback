package com.g4mesoft.captureplayback.timeline.delta;

import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;

public class GSEntryRemovedDelta extends GSEntryAddedDelta {

	public GSEntryRemovedDelta() {
	}

	public GSEntryRemovedDelta(UUID trackUUID, UUID entryUUID, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		super(trackUUID, entryUUID, startTime, endTime);
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
