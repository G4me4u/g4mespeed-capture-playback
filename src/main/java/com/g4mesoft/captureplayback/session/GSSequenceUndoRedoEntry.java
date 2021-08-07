package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public class GSSequenceUndoRedoEntry implements GSIUndoRedoEntry {

	private GSIDelta<GSSequence> delta;
	private long timestampMillis;

	GSSequenceUndoRedoEntry() {
	}

	public GSSequenceUndoRedoEntry(GSIDelta<GSSequence> delta) {
		this(delta, System.currentTimeMillis());
	}
	
	public GSSequenceUndoRedoEntry(GSIDelta<GSSequence> delta, long timestampMillis) {
		this.delta = delta;
		this.timestampMillis = timestampMillis;
	}
	
	private GSSequence getSequence(GSSession session) throws GSDeltaException {
		GSSequence sequence = session.get(GSSession.SEQUENCE);
		if (sequence == null)
			throw new GSDeltaException("Session does not contain a sequence");
		return sequence;
	}
	
	@Override
	public void undo(GSSession session) throws GSDeltaException {
		delta.unapply(getSequence(session));
	}

	@Override
	public void redo(GSSession session) throws GSDeltaException {
		delta.apply(getSequence(session));
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		delta = GSDeltaRegistries.SEQUENCE_DELTA_REGISTRY.read(buf);
		timestampMillis = buf.readLong();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSDeltaRegistries.SEQUENCE_DELTA_REGISTRY.write(buf, delta);
		buf.writeLong(timestampMillis);
	}
	
	@Override
	public long getTimestampMillis() {
		return timestampMillis;
	}
}
