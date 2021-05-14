package com.g4mesoft.captureplayback.stream;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSETickPhase;

import net.minecraft.util.math.BlockPos;

public final class GSSignalEvent implements Comparable<GSSignalEvent> {

	private final GSETickPhase phase;
	private final int microtick;
	private final int subordering;
	private final GSESignalEdge edge;
	private final BlockPos pos;
	private final boolean shadow;

	public GSSignalEvent(GSETickPhase phase, int microtick, int subordering,
			GSESignalEdge edge, BlockPos pos, boolean shadow) {
		
		this.phase = phase;
		this.microtick = (phase == GSETickPhase.BLOCK_EVENTS) ? microtick : -1;
		this.subordering = subordering;
		this.edge = edge;
		this.pos = pos;
		this.shadow = shadow;
	}
	
	public GSETickPhase getPhase() {
		return phase;
	}
	
	public int getMicrotick() {
		return microtick;
	}
	
	public int getSubordering() {
		return subordering;
	}
	
	public GSESignalEdge getEdge() {
		return edge;
	}

	public BlockPos getPos() {
		return pos;
	}
	
	public boolean isShadow() {
		return shadow;
	}
	
	@Override
	public int compareTo(GSSignalEvent other) {
		// Sort by tick phase, microtick, subordering, signal edge,
		// shadow versus real events, and finally the position.
		if (phase != other.phase)
			return phase.isBefore(other.phase) ? -1 : 1;
		if (microtick != other.microtick)
			return (microtick < other.microtick) ? -1 : 1;
		if (subordering != other.subordering)
			return (subordering < other.subordering) ? -1 : 1;
		if (edge != other.edge)
			return (edge == GSESignalEdge.RISING_EDGE ? -1 : 1);
		if (shadow != other.shadow)
			return !shadow ? -1 : 1;
		
		return pos.compareTo(other.pos);
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += 31 * hash + phase.hashCode();
		hash += 31 * hash + Integer.hashCode(microtick);
		hash += 31 * hash + Integer.hashCode(subordering);
		hash += 31 * hash + edge.hashCode();
		hash += 31 * hash + pos.hashCode();
		hash += 31 * hash + Boolean.hashCode(shadow);
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSSignalEvent))
			return false;
		
		GSSignalEvent event = (GSSignalEvent)other;

		if (phase != event.phase)
			return false;
		if (microtick != event.microtick)
			return false;
		if (subordering != event.subordering)
			return false;
		if (edge != event.edge)
			return false;
		if (!pos.equals(event.pos))
			return false;
		if (shadow != event.shadow)
			return false;
		
		return true;
	}
}
