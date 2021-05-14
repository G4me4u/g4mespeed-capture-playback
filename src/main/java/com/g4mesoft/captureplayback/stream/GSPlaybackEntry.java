package com.g4mesoft.captureplayback.stream;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSETickPhase;
import com.g4mesoft.captureplayback.common.GSSignalTime;

import net.minecraft.util.math.BlockPos;

public final class GSPlaybackEntry implements Comparable<GSPlaybackEntry> {

	private final long playbackTime;
	private final GSSignalEvent event;
	
	public GSPlaybackEntry(BlockPos pos, GSSignalTime time, int subordering, GSESignalEdge edge, boolean shadow) {
		playbackTime = time.getGametick();
		event = new GSSignalEvent(GSETickPhase.BLOCK_EVENTS, time.getMicrotick(), subordering, edge, pos, shadow);
	}
	
	public long getPlaybackTime() {
		return playbackTime;
	}
	
	public GSSignalEvent getEvent() {
		return event;
	}
	
	@Override
	public int compareTo(GSPlaybackEntry other) {
		if (playbackTime < other.playbackTime)
			return -1;
		if (playbackTime > other.playbackTime)
			return 1;
		return event.compareTo(other.event);
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += 31 * hash + Long.hashCode(playbackTime);
		hash += 31 * hash + event.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSPlaybackEntry))
			return false;
		
		GSPlaybackEntry entry = (GSPlaybackEntry)other;
		
		if (playbackTime != entry.playbackTime)
			return false;
		if (!event.equals(entry.event))
			return false;
		
		return true;
	}
}
