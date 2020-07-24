package com.g4mesoft.captureplayback.stream.playback;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.g4mesoft.captureplayback.common.GSEGameTickPhase;
import com.google.common.collect.Iterators;

public class GSPlaybackFrame {

	public static final GSPlaybackFrame EMPTY = new GSPlaybackFrame(Collections.emptyList());
	
	private final List<GSPlaybackEvent> events;
	
	GSPlaybackFrame(List<GSPlaybackEvent> events) {
		if (events == null)
			throw new IllegalArgumentException("events is null!");
		
		this.events = events;
	}
	
	public Iterator<GSPlaybackEvent> getPhaseIterator(GSEGameTickPhase phase) {
		return Iterators.unmodifiableIterator(events.iterator());
	}
}
