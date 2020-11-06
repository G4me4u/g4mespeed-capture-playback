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
		GSPlaybackFrame frame = GSPlaybackFrame.EMPTY;

		if (!isClosed() && isEventInFrame(events.peek())) {
			List<GSPlaybackEvent> frameEvents = new ArrayList<>();
	
			do {
				frameEvents.add(events.poll());
			} while (!isClosed() && isEventInFrame(events.peek()));

			frame = new GSPlaybackFrame(frameEvents);
		}

		playbackFrameIndex++;
		
		return frame;
	}
	
	private boolean isEventInFrame(GSPlaybackEvent event) {
		return (event.getTime().getGametick() <= playbackFrameIndex);
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
