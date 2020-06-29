package com.g4mesoft.captureplayback.common;

import java.io.IOException;

import net.minecraft.util.PacketByteBuf;

public final class GSBlockEventTime {

	public static final GSBlockEventTime ZERO = new GSBlockEventTime(0L, 0);
	public static final GSBlockEventTime INFINITY = new GSBlockEventTime(Long.MAX_VALUE, Integer.MAX_VALUE);
	
	private final long gametick;
	private final int microtick;
	
	public GSBlockEventTime(long gametick, int microtick) {
		if (gametick < 0L || microtick < 0)
			throw new IllegalArgumentException("Ticks must be non-negative!");
		
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
	public int hashCode() {
		return Long.hashCode(gametick) + 31 * microtick;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSBlockEventTime))
			return false;

		return isEqual((GSBlockEventTime)other);
	}
	
	public static GSBlockEventTime read(PacketByteBuf buf) throws IOException {
		long gametick = buf.readLong();
		int microtick = buf.readInt();
		
		if (gametick < 0L || microtick < 0)
			throw new IOException("Invalid time parameters!");
		
		return new GSBlockEventTime(gametick, microtick);
	}

	public static void write(PacketByteBuf buf, GSBlockEventTime time) throws IOException {
		buf.writeLong(time.gametick);
		buf.writeInt(time.microtick);
	}
}
