package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionUndoRedoEntry implements GSIUndoRedoEntry {

	private GSIDelta<GSComposition> delta;
	private long timestampMillis;

	GSCompositionUndoRedoEntry() {
	}

	public GSCompositionUndoRedoEntry(GSIDelta<GSComposition> delta) {
		this(delta, System.currentTimeMillis());
	}
	
	public GSCompositionUndoRedoEntry(GSIDelta<GSComposition> delta, long timestampMillis) {
		this.delta = delta;
		this.timestampMillis = timestampMillis;
	}
	
	private GSComposition getComposition(GSSession session) throws GSDeltaException {
		GSComposition composition = session.get(GSSession.COMPOSITION);
		if (composition == null)
			throw new GSDeltaException("Session does not contain a composition");
		return composition;
	}
	
	@Override
	public void undo(GSSession session) throws GSDeltaException {
		delta.unapply(getComposition(session));
	}

	@Override
	public void redo(GSSession session) throws GSDeltaException {
		delta.apply(getComposition(session));
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		delta = GSDeltaRegistries.COMPOSITION_DELTA_REGISTRY.read(buf);
		timestampMillis = buf.readLong();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSDeltaRegistries.COMPOSITION_DELTA_REGISTRY.write(buf, delta);
		buf.writeLong(timestampMillis);
	}
	
	@Override
	public long getTimestampMillis() {
		return timestampMillis;
	}
}
