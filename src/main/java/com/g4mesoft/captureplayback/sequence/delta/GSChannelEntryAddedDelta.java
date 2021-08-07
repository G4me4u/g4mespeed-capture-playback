package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public class GSChannelEntryAddedDelta extends GSChannelEntryDelta {

	private GSSignalTime startTime;
	private GSSignalTime endTime;

	public GSChannelEntryAddedDelta() {
	}

	public GSChannelEntryAddedDelta(GSChannelEntry entry) {
		this(entry.getParent().getChannelUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime());
	}
	
	public GSChannelEntryAddedDelta(UUID channelUUID, UUID entryUUID, GSSignalTime startTime, GSSignalTime endTime) {
		super(channelUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		removeEntry(sequence, startTime, endTime, GSChannelEntry.DEFAULT_ENTRY_TYPE);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		addEntry(sequence, startTime, endTime);
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
