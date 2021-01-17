package com.g4mesoft.captureplayback.common;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public final class GSSignalTime {

	public static final GSSignalTime ZERO = new GSSignalTime(0L, 0);
	public static final GSSignalTime INFINITY = new GSSignalTime(Long.MAX_VALUE, Integer.MAX_VALUE);
	
	private final long gametick;
	private final int microtick;

	public GSSignalTime(long gametick, int microtick) {
		if (gametick < 0L || microtick < 0)
			throw new IllegalArgumentException("Ticks must be non-negative!");
		
		this.gametick = gametick;
		this.microtick = microtick;
	}
	
	public GSSignalTime offsetCopy(long gtOffset, int mtOffset) {
		return new GSSignalTime(gametick + gtOffset, microtick + mtOffset);
	}
	
	public boolean isAfter(GSSignalTime other) {
		if (gametick == other.gametick)
			return (microtick > other.microtick);
		return (gametick > other.gametick);
	}

	public boolean isBefore(GSSignalTime other) {
		if (gametick == other.gametick)
			return (microtick < other.microtick);
		return (gametick < other.gametick);
	}

	public boolean isEqual(GSSignalTime other) {
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
		if (!(other instanceof GSSignalTime))
			return false;

		return isEqual((GSSignalTime)other);
	}
	
	public static GSSignalTime read(PacketByteBuf buf) throws IOException {
		long gametick = buf.readLong();
		int microtick = buf.readInt();
		
		if (gametick < 0L || microtick < 0)
			throw new IOException("Invalid time parameters!");
		
		return new GSSignalTime(gametick, microtick);
	}

	public static void write(PacketByteBuf buf, GSSignalTime time) throws IOException {
		buf.writeLong(time.gametick);
		buf.writeInt(time.microtick);
	}
}
