package com.g4mesoft.captureplayback.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.g4mesoft.captureplayback.common.GSAbstractStream;
import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSETickPhase;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.stream.frame.GSBasicSignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

import net.minecraft.util.math.BlockPos;

public abstract class GSPlaybackStream extends GSAbstractStream implements GSIPlaybackStream {
	
	private final GSBlockRegion blockRegion;
	private final PriorityQueue<GSPlaybackEntry> entries;

	private long playbackTime;
	
	private boolean wasCloseCalled;
	
	public GSPlaybackStream(GSBlockRegion blockRegion) {
		this.blockRegion = blockRegion;
		entries = new PriorityQueue<>();
	}
	
	protected void addChannelPlayback(GSChannel channel, long offset) {
		for (BlockPos position : channel.getInfo().getPositions()) {
			for (GSChannelEntry entry : channel.getEntries()) {
				GSEChannelEntryType type = entry.getType();
				
				GSSignalTime startTime, endTime;
				if (offset != 0L) {
					startTime = entry.getStartTime().offsetCopy(offset, 0);
					endTime = entry.getEndTime().offsetCopy(offset, 0);
				} else {
					startTime = entry.getStartTime();
					endTime = entry.getEndTime();
				}
			
				addEntry(position, startTime, GSESignalEdge.RISING_EDGE, !type.hasStartEvent());
				addEntry(position, endTime, GSESignalEdge.FALLING_EDGE, !type.hasEndEvent());
			}
		}
	}
	
	protected void addEntry(BlockPos pos, GSSignalTime time, GSESignalEdge edge, boolean shadow) {
		entries.add(new GSPlaybackEntry(pos, time, entries.size(), edge, shadow));
	}
	
	private GSSignalEvent toCleanupEvent(GSSignalEvent event) {
		return new GSSignalEvent(GSETickPhase.IMMEDIATE, -1, 0, event.getEdge(), event.getPos(), true);
	}

	@Override
	public GSISignalFrame read() {
		GSISignalFrame frame = GSISignalFrame.EMPTY;

		if (!isClosed()) {
			if (wasCloseCalled) {
				List<GSSignalEvent> frameEvents = new ArrayList<>();
				
				// Add all entries as immediate shadow events to
				// ensure that powered positions are removed.
				GSPlaybackEntry entry;
				while ((entry = entries.poll()) != null)
					frameEvents.add(toCleanupEvent(entry.getEvent()));
				frame = new GSBasicSignalFrame(frameEvents);
			} else if (isEntryInFrame(entries.peek())) {
				List<GSSignalEvent> frameEvents = new ArrayList<>();
	
				do {
					frameEvents.add(entries.poll().getEvent());
				} while (!isClosed() && isEntryInFrame(entries.peek()));
	
				frame = new GSBasicSignalFrame(frameEvents);
			}
		}

		playbackTime++;

		if (isClosed())
			dispatchCloseEvent();

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
	public void close() {
		wasCloseCalled = true;
	}
	
	@Override
	public boolean isClosed() {
		return entries.isEmpty();
	}
	
	@Override
	public boolean isForceClosed() {
		return isClosed() && wasCloseCalled;
	}
}
