package com.g4mesoft.captureplayback.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSETickPhase;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.captureplayback.stream.GSPlaybackEntry;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;
import com.g4mesoft.captureplayback.stream.frame.GSBasicSignalFrame;
import com.g4mesoft.captureplayback.stream.frame.GSISignalFrame;

import net.minecraft.util.math.BlockPos;

class GSSequencePlaybackStream implements GSIPlaybackStream {

	private final GSBlockRegion blockRegion;
	private final PriorityQueue<GSPlaybackEntry> entries;

	private long playbackTime;
	private boolean closing;

	public GSSequencePlaybackStream(GSSequence sequence) {
		blockRegion = sequence.getBlockRegion();
		entries = new PriorityQueue<>();

		for (GSChannel channel : sequence.getChannels()) {
			for (BlockPos position : channel.getInfo().getPositions()) {
				for (GSChannelEntry entry : channel.getEntries()) {
					GSEChannelEntryType type = entry.getType();
					
					addEntry(position, entry.getStartTime(), GSESignalEdge.RISING_EDGE, !type.hasStartEvent());
					addEntry(position, entry.getEndTime(), GSESignalEdge.FALLING_EDGE, !type.hasEndEvent());
				}
			}
		}
	}
	
	private void addEntry(BlockPos pos, GSSignalTime time, GSESignalEdge edge, boolean shadow) {
		entries.add(new GSPlaybackEntry(pos, time, entries.size(), edge, shadow));
	}
	
	private GSSignalEvent toCleanupEvent(GSSignalEvent event) {
		return new GSSignalEvent(GSETickPhase.IMMEDIATE, -1, 0, event.getEdge(), event.getPos(), true);
	}

	@Override
	public GSISignalFrame read() {
		GSISignalFrame frame = GSISignalFrame.EMPTY;

		if (!isClosed()) {
			if (closing) {
				List<GSSignalEvent> frameEvents = new ArrayList<>();
				
				// Add all entries as immediate shadow events to
				// ensure that powered positions are removed.
				for (GSPlaybackEntry entry : entries)
					frameEvents.add(toCleanupEvent(entry.getEvent()));
				entries.clear();
				
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
		closing = true;
	}

	@Override
	public boolean isClosed() {
		return entries.isEmpty();
	}
}
