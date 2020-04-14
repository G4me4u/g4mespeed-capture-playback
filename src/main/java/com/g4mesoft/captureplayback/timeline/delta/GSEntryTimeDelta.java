package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryTimeDelta extends GSEntryDelta {

	private GSBlockEventTime newStartTime;
	private GSBlockEventTime newEndTime;

	private GSBlockEventTime oldStartTime;
	private GSBlockEventTime oldEndTime;

	public GSEntryTimeDelta() {
	}
	
	public GSEntryTimeDelta(UUID trackUUID, UUID entryUUID, GSBlockEventTime newStartTime,
			GSBlockEventTime newEndTime, GSBlockEventTime oldStartTime, GSBlockEventTime oldEndTime) {

		super(trackUUID, entryUUID);
		
		this.newStartTime = newStartTime;
		this.newEndTime = newEndTime;

		this.oldStartTime = oldStartTime;
		this.oldEndTime = oldEndTime;
	}
	
	private void setEntryTime(GSBlockEventTime newStartTime, GSBlockEventTime newEndTime,
			GSBlockEventTime oldStartTime, GSBlockEventTime oldEndTime, GSTimeline timeline) throws GSTimelineDeltaException {

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
		
		newStartTime = GSBlockEventTime.read(buf);
		newEndTime = GSBlockEventTime.read(buf);
		oldStartTime = GSBlockEventTime.read(buf);
		oldEndTime = GSBlockEventTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSBlockEventTime.write(buf, newStartTime);
		GSBlockEventTime.write(buf, newEndTime);
		GSBlockEventTime.write(buf, oldStartTime);
		GSBlockEventTime.write(buf, oldEndTime);
	}
}
