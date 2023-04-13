package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public interface GSIUndoRedoEntry {

	public static final long MAX_CHAINED_INTERVAL = 250L;
	
	public void undo(GSSession session) throws GSDeltaException;
	
	public void redo(GSSession session) throws GSDeltaException;
	
	public void read(GSDecodeBuffer buf) throws IOException;

	public void write(GSEncodeBuffer buf) throws IOException;
	
	public long getTimestampMillis();
	
	default public boolean isChained(GSIUndoRedoEntry other) {
		return Math.abs(other.getTimestampMillis() - getTimestampMillis()) <= MAX_CHAINED_INTERVAL;
	}
}
