package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackUndoRedoHistoryDelta implements GSIDelta<GSUndoRedoHistory> {

	private GSIUndoRedoEntry entry;
	
	public GSTrackUndoRedoHistoryDelta() {
	}

	public GSTrackUndoRedoHistoryDelta(GSIUndoRedoEntry entry) {
		this.entry = entry;
	}
	
	@Override
	public void apply(GSUndoRedoHistory history) throws GSDeltaException {
		history.addEntry(entry);
	}

	@Override
	public void unapply(GSUndoRedoHistory history) throws GSDeltaException {
		throw new GSDeltaException("Unapply unsupported");
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		entry = GSUndoRedoHistory.readEntry(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSUndoRedoHistory.writeEntry(buf, entry);
	}
}
