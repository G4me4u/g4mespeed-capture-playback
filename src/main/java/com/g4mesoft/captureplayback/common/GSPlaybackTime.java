package com.g4mesoft.captureplayback.common;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public final class GSPlaybackTime {

	public static final GSPlaybackTime ZERO = new GSPlaybackTime(0L, 0);
	public static final GSPlaybackTime INFINITY = new GSPlaybackTime(Long.MAX_VALUE, Integer.MAX_VALUE);
	
	private final long gametick;
	private final int microtick;
	
	public GSPlaybackTime(long gametick, int microtick) {
		if (gametick < 0L || microtick < 0)
			throw new IllegalArgumentException("Ticks must be non-negative!");
		
		this.gametick = gametick;
		this.microtick = microtick;
	}
	
	public GSPlaybackTime offsetCopy(long gtOffset, int mtOffset) {
		return new GSPlaybackTime(gametick + gtOffset, microtick + mtOffset);
	}
	
	public boolean isAfter(GSPlaybackTime other) {
		if (gametick == other.gametick)
			return (microtick > other.microtick);
		return (gametick > other.gametick);
	}

	public boolean isBefore(GSPlaybackTime other) {
		if (gametick == other.gametick)
			return (microtick < other.microtick);
		return (gametick < other.gametick);
	}

	public boolean isEqual(GSPlaybackTime other) {
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
		if (!(other instanceof GSPlaybackTime))
			return false;

		return isEqual((GSPlaybackTime)other);
	}
	
	public static GSPlaybackTime read(PacketByteBuf buf) throws IOException {
		long gametick = buf.readLong();
		int microtick = buf.readInt();
		
		if (gametick < 0L || microtick < 0)
			throw new IOException("Invalid time parameters!");
		
		return new GSPlaybackTime(gametick, microtick);
	}

	public static void write(PacketByteBuf buf, GSPlaybackTime time) throws IOException {
		buf.writeLong(time.gametick);
		buf.writeInt(time.microtick);
	}
}
