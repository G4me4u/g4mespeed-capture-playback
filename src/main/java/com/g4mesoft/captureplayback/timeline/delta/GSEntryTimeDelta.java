package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryTimeDelta extends GSEntryDelta {

	private GSSignalTime newStartTime;
	private GSSignalTime newEndTime;

	private GSSignalTime oldStartTime;
	private GSSignalTime oldEndTime;

	public GSEntryTimeDelta() {
	}
	
	public GSEntryTimeDelta(UUID trackUUID, UUID entryUUID, GSSignalTime newStartTime,
			GSSignalTime newEndTime, GSSignalTime oldStartTime, GSSignalTime oldEndTime) {

		super(trackUUID, entryUUID);
		
		this.newStartTime = newStartTime;
		this.newEndTime = newEndTime;

		this.oldStartTime = oldStartTime;
		this.oldEndTime = oldEndTime;
	}
	
	private void setEntryTime(GSSignalTime newStartTime, GSSignalTime newEndTime,
			GSSignalTime oldStartTime, GSSignalTime oldEndTime, GSTimeline timeline) throws GSTimelineDeltaException {

		GSTrackEntry entry = getEntry(timeline);
		checkEntryTimespan(entry, oldStartTime, oldEndTime);
		entry.setTimespan(newStartTime, newEndTime);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setEntryTime(oldStartTime, oldEndTime, newStartTime, newEndTime, timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setEntryTime(newStartTime, newEndTime, oldStartTime, oldEndTime, timeline);
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
