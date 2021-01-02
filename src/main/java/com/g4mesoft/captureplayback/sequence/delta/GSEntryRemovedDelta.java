package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryRemovedDelta extends GSEntryDelta {

	private GSSignalTime startTime;
	private GSSignalTime endTime;
	private GSEChannelEntryType type;
	
	public GSEntryRemovedDelta() {
	}

	public GSEntryRemovedDelta(GSChannelEntry entry) {
		this(entry.getParent().getChannelUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime(), entry.getType());
	}
	
	public GSEntryRemovedDelta(UUID channelUUID, UUID entryUUID, GSSignalTime startTime,
			GSSignalTime endTime, GSEChannelEntryType type) {
		
		super(channelUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
		this.type = type;
	}

	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		GSChannelEntry entry = addEntry(sequence, entryUUID, startTime, endTime);
		
		entry.setType(type);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		removeEntry(sequence, entryUUID, startTime, endTime, type);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		startTime = GSSignalTime.read(buf);
		endTime = GSSignalTime.read(buf);
		
		type = GSEChannelEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Type index invalid");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSSignalTime.write(buf, startTime);
		GSSignalTime.write(buf, endTime);
		
		buf.writeInt(type.getIndex());
	}
}
