package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryAddedDelta extends GSEntryDelta {

	private GSSignalTime startTime;
	private GSSignalTime endTime;

	public GSEntryAddedDelta() {
	}

	public GSEntryAddedDelta(GSChannelEntry entry) {
		this(entry.getParent().getChannelUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime());
	}
	
	public GSEntryAddedDelta(UUID channelUUID, UUID entryUUID, GSSignalTime startTime, GSSignalTime endTime) {
		super(channelUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		removeEntry(sequence, entryUUID, startTime, endTime, GSChannelEntry.DEFAULT_ENTRY_TYPE);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		addEntry(sequence, entryUUID, startTime, endTime);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		startTime = GSSignalTime.read(buf);
		endTime = GSSignalTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSSignalTime.write(buf, startTime);
		GSSignalTime.write(buf, endTime);
	}
}
