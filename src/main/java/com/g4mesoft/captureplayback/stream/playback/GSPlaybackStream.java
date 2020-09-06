package com.g4mesoft.captureplayback.stream.playback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSIReadableStream;

public class GSPlaybackStream implements GSIReadableStream<GSPlaybackFrame> {

	private final GSBlockRegion blockRegion;
	private final PriorityQueue<GSPlaybackEvent> events;
	
	private long playbackFrameIndex;
	
	public GSPlaybackStream(GSBlockRegion blockRegion, Collection<GSPlaybackEvent> events) {
		this.blockRegion = blockRegion;
		this.events = new PriorityQueue<GSPlaybackEvent>(events);
	
		playbackFrameIndex = 0L;
	}
	
	@Override
	public GSPlaybackFrame read() {
		List<GSPlaybackEvent> frameEvents = null;

		GSPlaybackEvent event;
		while ((event = events.peek()) != null && event.getTime().getGametick() == playbackFrameIndex) {
			if (frameEvents == null)
				frameEvents = new ArrayList<>();
			frameEvents.add(events.poll());
		}
		
		playbackFrameIndex++;
		
		if (frameEvents == null)
			return GSPlaybackFrame.EMPTY;
		
		return new GSPlaybackFrame(frameEvents);
	}
	
	@Override
	public GSBlockRegion getBlockRegion() {
		return blockRegion;
	}

	@Override
	public boolean isClosed() {
		return events.isEmpty();
	}
}
