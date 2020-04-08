package com.g4mesoft.planner.timeline;

public class GSBlockEventTime {

	public static final GSBlockEventTime ZERO = new GSBlockEventTime(0L, 0);
	public static final GSBlockEventTime INFINITY = new GSBlockEventTime(Long.MAX_VALUE, Integer.MAX_VALUE);
	
	private final long gametick;
	private final int microtick;
	
	public GSBlockEventTime(long gametick, int microtick) {
		this.gametick = gametick;
		this.microtick = microtick;
	}

	public GSBlockEventTime offsetCopy(long gtOffset, int mtOffset) {
		return new GSBlockEventTime(gametick + gtOffset, microtick + mtOffset);
	}
	
	public boolean isAfter(GSBlockEventTime other) {
		if (gametick == other.gametick)
			return (microtick > other.microtick);
		return (gametick > other.gametick);
	}

	public boolean isBefore(GSBlockEventTime other) {
		if (gametick == other.gametick)
			return (microtick < other.microtick);
		return (gametick < other.gametick);
	}

	public boolean isEqual(GSBlockEventTime other) {
		return (gametick == other.gametick && microtick == other.microtick);
	}
	
	public long getGametick() {
		return gametick;
	}
	
	public int getMicrotick() {
		return microtick;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GSBlockEventTime)
			return isEqual((GSBlockEventTime)other);
		return false;
	}
}
