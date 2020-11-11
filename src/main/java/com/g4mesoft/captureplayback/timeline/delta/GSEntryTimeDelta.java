package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSPlaybackTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryTimeDelta extends GSEntryDelta {

	private GSPlaybackTime newStartTime;
	private GSPlaybackTime newEndTime;

	private GSPlaybackTime oldStartTime;
	private GSPlaybackTime oldEndTime;

	public GSEntryTimeDelta() {
	}
	
	public GSEntryTimeDelta(UUID trackUUID, UUID entryUUID, GSPlaybackTime newStartTime,
			GSPlaybackTime newEndTime, GSPlaybackTime oldStartTime, GSPlaybackTime oldEndTime) {

		super(trackUUID, entryUUID);
		
		this.newStartTime = newStartTime;
		this.newEndTime = newEndTime;

		this.oldStartTime = oldStartTime;
		this.oldEndTime = oldEndTime;
	}
	
	private void setEntryTime(GSPlaybackTime newStartTime, GSPlaybackTime newEndTime,
			GSPlaybackTime oldStartTime, GSPlaybackTime oldEndTime, GSTimeline timeline) throws GSTimelineDeltaException {

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
		
		newStartTime = GSPlaybackTime.read(buf);
		newEndTime = GSPlaybackTime.read(buf);
		oldStartTime = GSPlaybackTime.read(buf);
		oldEndTime = GSPlaybackTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSPlaybackTime.write(buf, newStartTime);
		GSPlaybackTime.write(buf, newEndTime);
		GSPlaybackTime.write(buf, oldStartTime);
		GSPlaybackTime.write(buf, oldEndTime);
	}
}
