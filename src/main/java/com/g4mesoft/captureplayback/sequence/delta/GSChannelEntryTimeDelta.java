package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public class GSChannelEntryTimeDelta extends GSChannelEntryDelta {

	private GSSignalTime newStartTime;
	private GSSignalTime newEndTime;

	private GSSignalTime oldStartTime;
	private GSSignalTime oldEndTime;

	public GSChannelEntryTimeDelta() {
	}
	
	public GSChannelEntryTimeDelta(UUID channelUUID, UUID entryUUID, GSSignalTime newStartTime,
			GSSignalTime newEndTime, GSSignalTime oldStartTime, GSSignalTime oldEndTime) {

		super(channelUUID, entryUUID);
		
		this.newStartTime = newStartTime;
		this.newEndTime = newEndTime;

		this.oldStartTime = oldStartTime;
		this.oldEndTime = oldEndTime;
	}
	
	private void setEntryTime(GSSignalTime newStartTime, GSSignalTime newEndTime,
			GSSignalTime oldStartTime, GSSignalTime oldEndTime, GSSequence sequence) throws GSSequenceDeltaException {

		GSChannelEntry entry = getEntry(sequence);
		checkEntryTimespan(entry, oldStartTime, oldEndTime);
		entry.setTimespan(newStartTime, newEndTime);
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		setEntryTime(oldStartTime, oldEndTime, newStartTime, newEndTime, sequence);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		setEntryTime(newStartTime, newEndTime, oldStartTime, oldEndTime, sequence);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newStartTime = GSSignalTime.read(buf);
		newEndTime = GSSignalTime.read(buf);
		oldStartTime = GSSignalTime.read(buf);
		oldEndTime = GSSignalTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSSignalTime.write(buf, newStartTime);
		GSSignalTime.write(buf, newEndTime);
		GSSignalTime.write(buf, oldStartTime);
		GSSignalTime.write(buf, oldEndTime);
	}
}
