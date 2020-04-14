package com.g4mesoft.captureplayback.playback;

import java.util.ArrayList;
import java.util.List;

import com.g4mesoft.captureplayback.timeline.GSTimeline;

public class GSPlaylistTrack {

	private final GSTimeline timeline;
	private final List<Integer> positions;
	
	public GSPlaylistTrack(GSTimeline timeline) {
		this.timeline = timeline;
		this.positions = new ArrayList<Integer>();
	}
	
	
	
}
