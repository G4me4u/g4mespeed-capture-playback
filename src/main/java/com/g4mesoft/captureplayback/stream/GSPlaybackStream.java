package com.g4mesoft.captureplayback.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import com.g4mesoft.captureplayback.stream.frame.GSBasicSignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

public class GSPlaybackStream implements GSIReadableStream<GSISignalFrame> {

	private final GSBlockRegion blockRegion;
	private final PriorityQueue<GSSignalEvent> events;
	
	private long playbackFrameIndex;
	
	public GSPlaybackStream(GSBlockRegion blockRegion, Collection<GSSignalEvent> events) {
		this.blockRegion = blockRegion;
		this.events = new PriorityQueue<GSSignalEvent>(events);
	
		playbackFrameIndex = 0L;
	}
	
	@Override
	public GSISignalFrame read() {
		GSISignalFrame frame = GSISignalFrame.EMPTY;

		if (!isClosed() && isEventInFrame(events.peek())) {
			List<GSSignalEvent> frameEvents = new ArrayList<>();
	
			do {
				frameEvents.add(events.poll());
			} while (!isClosed() && isEventInFrame(events.peek()));

			frame = new GSBasicSignalFrame(frameEvents);
		}

		playbackFrameIndex++;
		
		return frame;
	}
	
	private boolean isEventInFrame(GSSignalEvent event) {
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
