package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryAddedDelta extends GSEntryDelta {

	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;

	public GSEntryAddedDelta() {
	}
	
	public GSEntryAddedDelta(UUID trackUUID, UUID entryUUID, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		super(trackUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrackEntry entry = getEntry(timeline);
		checkEntryTimespan(entry, startTime, endTime);
		entry.getTrack().removeEntry(entryUUID);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = getTrack(timeline);
		
		if (track.hasEntryUUID(entryUUID))
			throw new GSTimelineDeltaException("Entry already exists");
		if (track.tryAddEntry(entryUUID, startTime, endTime) == null)
			throw new GSTimelineDeltaException("Unable to add entry");
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		startTime = GSBlockEventTime.read(buf);
		endTime = GSBlockEventTime.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSBlockEventTime.write(buf, startTime);
		GSBlockEventTime.write(buf, endTime);
	}
}
