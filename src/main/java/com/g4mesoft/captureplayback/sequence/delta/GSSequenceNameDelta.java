package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSSequenceNameDelta implements GSISequenceDelta {

	private String newName;
	private String oldName;

	public GSSequenceNameDelta() {
	}
	
	public GSSequenceNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSSequence sequence) throws GSDeltaException {
		if (!Objects.equals(oldName, sequence.getName()))
			throw new GSDeltaException("Sequence does not have the expected name");
		sequence.setName(newName);
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSDeltaException {
		setName(oldName, newName, sequence);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSDeltaException {
		setName(newName, oldName, sequence);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		newName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		oldName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
