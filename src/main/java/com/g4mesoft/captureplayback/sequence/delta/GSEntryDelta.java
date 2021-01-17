package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public abstract class GSEntryDelta extends GSChannelDelta {

	protected UUID entryUUID;

	protected GSEntryDelta() {
	}
	
	protected GSEntryDelta(UUID channelUUID, UUID entryUUID) {
		super(channelUUID);
		
		this.entryUUID = entryUUID;
	}

	protected GSChannelEntry getEntry(GSSequence sequence) throws GSSequenceDeltaException {
		return getEntry(getChannel(sequence));
	}
	
	protected GSChannelEntry getEntry(GSChannel channel) throws GSSequenceDeltaException {
		GSChannelEntry entry = channel.getEntry(entryUUID);
		if (entry == null)
			throw new GSSequenceDeltaException("Expected entry does not exist");
		
		return entry;
	}
	
	protected void checkEntryTimespan(GSChannelEntry entry, GSSignalTime startTime,
			GSSignalTime endTime) throws GSSequenceDeltaException {
		
		if (!entry.getStartTime().isEqual(startTime) || !entry.getEndTime().isEqual(endTime))
			throw new GSSequenceDeltaException("Entry does not have the expected timespan");
	}
	
	protected void checkEntryType(GSChannelEntry entry, GSEChannelEntryType type) throws GSSequenceDeltaException {
		if (entry.getType() != type)
			throw new GSSequenceDeltaException("Entry does not have the expected type");
	}
	
	protected void removeEntry(GSSequence sequence, UUID entryUUID, GSSignalTime startTime, 
			GSSignalTime endTime, GSEChannelEntryType expectedType) throws GSSequenceDeltaException {

		GSChannelEntry entry = getEntry(sequence);
		checkEntryTimespan(entry, startTime, endTime);
		checkEntryType(entry, expectedType);
		entry.getParent().removeEntry(entryUUID);
	}
	
	protected GSChannelEntry addEntry(GSSequence sequence, UUID entryUUID, GSSignalTime startTime,
			GSSignalTime endTime) throws GSSequenceDeltaException {
		
		GSChannel channel = getChannel(sequence);
		if (channel.hasEntryUUID(entryUUID))
			throw new GSSequenceDeltaException("Entry already exists");

		GSChannelEntry entry = channel.tryAddEntry(entryUUID, startTime, endTime);
		if (entry == null)
			throw new GSSequenceDeltaException("Unable to add entry");
		
		return entry;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		entryUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
		
		buf.writeUuid(entryUUID);
	}
}
