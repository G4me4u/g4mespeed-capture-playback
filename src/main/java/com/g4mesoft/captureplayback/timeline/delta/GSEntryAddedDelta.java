package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSPlaybackTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryAddedDelta extends GSEntryDelta {

	private GSPlaybackTime startTime;
	private GSPlaybackTime endTime;

	public GSEntryAddedDelta() {
	}

	public GSEntryAddedDelta(GSTrackEntry entry) {
		this(entry.getOwnerTrack().getTrackUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime());
	}
	
	public GSEntryAddedDelta(UUID trackUUID, UUID entryUUID, GSPlaybackTime startTime, GSPlaybackTime endTime) {
		super(trackUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		removeEntry(timeline, entryUUID, startTime, endTime, GSTrackEntry.DEFAULT_ENTRY_TYPE);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		addEntry(timeline, entryUUID, startTime, endTime);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		startTime = GSPlaybackTime.read(buf);
		endTime = GSPlaybackTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSPlaybackTime.write(buf, startTime);
		GSPlaybackTime.write(buf, endTime);
	}
}
