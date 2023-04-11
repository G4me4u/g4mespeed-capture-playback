package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

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
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newPrevUUID = buf.readBoolean() ? buf.readUUID() : null;
		oldPrevUUID = buf.readBoolean() ? buf.readUUID() : null;
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeBoolean(newPrevUUID != null);
		if (newPrevUUID != null)
			buf.writeUUID(newPrevUUID);
		buf.writeBoolean(oldPrevUUID != null);
		if (oldPrevUUID != null)
			buf.writeUUID(oldPrevUUID);
	}
}
