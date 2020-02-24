package com.g4mesoft.planner.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSTimelineTable {

	private final List<GSTimeline> timelines;
	
	public GSTimelineTable() {
		this.timelines = new ArrayList<GSTimeline>();
	}
	
	public GSTimeline addTimeline(GSTimelineInfo info) {
		GSTimeline timeline = new GSTimeline(info);
		timelines.add(timeline);
		return timeline;
	}
	
	public List<GSTimeline> getTimelines() {
		return Collections.unmodifiableList(timelines);
	}
}
