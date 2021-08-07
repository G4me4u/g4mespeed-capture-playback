package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;

import net.minecraft.network.PacketByteBuf;

public interface GSIUndoRedoEntry {

	public static final long MAX_CHAINED_INTERVAL = 250L;
	
	public void undo(GSSession session) throws GSDeltaException;
	
	public void redo(GSSession session) throws GSDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;
	
	public long getTimestampMillis();
	
	default public boolean isChained(GSIUndoRedoEntry other) {
		return Math.abs(other.getTimestampMillis() - getTimestampMillis()) <= MAX_CHAINED_INTERVAL;
	}
}
