package com.g4mesoft.captureplayback.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import com.g4mesoft.captureplayback.stream.frame.GSBasicSignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

public class GSPlaybackStream implements GSIReadableStream<GSISignalFrame> {

	private final GSBlockRegion blockRegion;
	private final PriorityQueue<GSPlaybackEntry> entries;
	
	private long playbackTime;
	
	public GSPlaybackStream(GSBlockRegion blockRegion, Collection<GSPlaybackEntry> entries) {
		this.blockRegion = blockRegion;
		this.entries = new PriorityQueue<GSPlaybackEntry>(entries);
	
		playbackTime = 0L;
	}
	
	@Override
	public GSISignalFrame read() {
		GSISignalFrame frame = GSISignalFrame.EMPTY;

		if (!isClosed() && isEntryInFrame(entries.peek())) {
			List<GSSignalEvent> frameEvents = new ArrayList<>();
	
			do {
				frameEvents.add(entries.poll().getEvent());
			} while (!isClosed() && isEntryInFrame(entries.peek()));

			frame = new GSBasicSignalFrame(frameEvents);
		}

		playbackTime++;
		
		return frame;
	}
	
	private boolean isEntryInFrame(GSPlaybackEntry entry) {
		return (entry.getPlaybackTime() <= playbackTime);
	}
	
	@Override
	public GSBlockRegion getBlockRegion() {
		return blockRegion;
	}

	@Override
	public boolean isClosed() {
		return entries.isEmpty();
	}
}
