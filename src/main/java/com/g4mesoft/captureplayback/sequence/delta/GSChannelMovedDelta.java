package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public class GSChannelMovedDelta extends GSChannelDelta {

	private UUID newPrevUUID;
	private UUID oldPrevUUID;

	public GSChannelMovedDelta() {
	}

	public GSChannelMovedDelta(UUID channelUUID, UUID newPrevUUID, UUID oldPrevUUID) {
		super(channelUUID);
		
		this.newPrevUUID = newPrevUUID;
		this.oldPrevUUID = oldPrevUUID;
	}
	
	private void moveChannel(UUID newPrevUUID, UUID oldPrevUUID, GSSequence sequence) throws GSDeltaException {
		checkPreviousChannel(getChannel(sequence), oldPrevUUID);
		sequence.moveChannelAfter(channelUUID, newPrevUUID);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		moveChannel(oldPrevUUID, newPrevUUID, sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		moveChannel(newPrevUUID, oldPrevUUID, sequence);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newPrevUUID = buf.readBoolean() ? buf.readUuid() : null;
		oldPrevUUID = buf.readBoolean() ? buf.readUuid() : null;
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeBoolean(newPrevUUID != null);
		if (newPrevUUID != null)
			buf.writeUuid(newPrevUUID);
		buf.writeBoolean(oldPrevUUID != null);
		if (oldPrevUUID != null)
			buf.writeUuid(oldPrevUUID);
	}
}
