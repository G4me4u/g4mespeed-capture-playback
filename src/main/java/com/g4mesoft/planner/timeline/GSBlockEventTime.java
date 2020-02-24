package com.g4mesoft.planner.timeline;

public class GSBlockEventTime {

	public static final GSBlockEventTime ZERO = new GSBlockEventTime(0L, 0);
	public static final GSBlockEventTime INFINITE = new GSBlockEventTime(Long.MAX_VALUE, Integer.MAX_VALUE);
	
	private final long gameTime;
	private final int blockEventDelay;
	
	public GSBlockEventTime(long gameTime, int blockEventDelay) {
		this.gameTime = gameTime;
		this.blockEventDelay = blockEventDelay;
	}
	
	public boolean isAfter(GSBlockEventTime other) {
		if (gameTime == other.gameTime)
			return (blockEventDelay > other.blockEventDelay);
		return (gameTime > other.gameTime);
	}

	public boolean isBefore(GSBlockEventTime other) {
		if (gameTime == other.gameTime)
			return (blockEventDelay < other.blockEventDelay);
		return (gameTime < other.gameTime);
	}

	public boolean isEqual(GSBlockEventTime other) {
		return !isAfter(other) && !isBefore(other);
	}
	
	public long getGameTime() {
		return gameTime;
	}
	
	public int getBlockEventDelay() {
		return blockEventDelay;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GSBlockEventTime)
			return isEqual((GSBlockEventTime)other);
		return false;
	}
}
