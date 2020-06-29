package com.g4mesoft.captureplayback.stream.playback;

import com.g4mesoft.captureplayback.common.GSBlockEventTime;
import com.g4mesoft.captureplayback.common.GSESignalEdge;

import net.minecraft.util.math.BlockPos;

public final class GSPlaybackEvent implements Comparable<GSPlaybackEvent> {

	private final BlockPos pos;
	private final GSBlockEventTime time;
	private final GSESignalEdge edge;
	
	public GSPlaybackEvent(BlockPos pos, GSBlockEventTime time, GSESignalEdge edge) {
		this.pos = pos;
		this.time = time;
		this.edge = edge;
	}
	
	public BlockPos getPos() {
		return pos;
	}
	
	public GSBlockEventTime getTime() {
		return time;
	}
	
	public GSESignalEdge getEdge() {
		return edge;
	}

	@Override
	public int compareTo(GSPlaybackEvent other) {
		if (time.isEqual(other.time)) {
			int pc = pos.compareTo(other.pos);
		
			if (pc == 0) {
				if (edge == other.edge)
					return 0;
				
				// Ensure that if we have two events at the same time,
				// that the rising edge will be before the falling edge.
				return (edge == GSESignalEdge.RISING_EDGE ? -1 : 1);
			}
			
			return pc;
		}
		
		return (time.isBefore(other.time) ? -1 : 1);
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += 31 * hash + pos.hashCode();
		hash += 31 * hash + time.hashCode();
		hash += 31 * hash + edge.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSPlaybackEvent))
			return false;
		
		GSPlaybackEvent event = (GSPlaybackEvent)other;

		if (edge != event.edge)
			return false;
		if (!time.isEqual(event.time))
			return false;
		if (!pos.equals(event.pos))
			return false;
		
		return true;
	}
}
