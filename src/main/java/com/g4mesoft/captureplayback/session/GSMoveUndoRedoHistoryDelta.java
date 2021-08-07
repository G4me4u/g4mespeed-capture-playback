package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;

import net.minecraft.network.PacketByteBuf;

public class GSMoveUndoRedoHistoryDelta implements GSIDelta<GSUndoRedoHistory> {

	private boolean moveToRedo;
	private int count;
	
	public GSMoveUndoRedoHistoryDelta() {
	}

	public GSMoveUndoRedoHistoryDelta(boolean moveToRedo, int count) {
		this.moveToRedo = moveToRedo;
		this.count = count;
	}
	
	@Override
	public void apply(GSUndoRedoHistory history) throws GSDeltaException {
		if (history.moveEntries(moveToRedo, count) != count)
			throw new GSDeltaException("Unable to move " + count + " entries");
	}

	@Override
	public void unapply(GSUndoRedoHistory history) throws GSDeltaException {
		throw new GSDeltaException("Unapply unsupported");
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		moveToRedo = buf.readBoolean();
		count = buf.readInt();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeBoolean(moveToRedo);
		buf.writeInt(count);
	}
}
