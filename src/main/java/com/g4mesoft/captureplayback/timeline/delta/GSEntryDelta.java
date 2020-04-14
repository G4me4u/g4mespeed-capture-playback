package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public abstract class GSEntryDelta extends GSTrackDelta {

	protected UUID entryUUID;

	protected GSEntryDelta() {
	}
	
	protected GSEntryDelta(UUID trackUUID, UUID entryUUID) {
		super(trackUUID);
		
		this.entryUUID = entryUUID;
	}

	protected GSTrackEntry getEntry(GSTimeline timeline) throws GSTimelineDeltaException {
		return getEntry(getTrack(timeline));
	}
	
	protected GSTrackEntry getEntry(GSTrack track) throws GSTimelineDeltaException {
		GSTrackEntry entry = track.getEntry(entryUUID);
		if (entry == null)
			throw new GSTimelineDeltaException("Expected entry does not exist");
		
		return entry;
	}
	
	protected void checkEntryTimespan(GSTrackEntry entry, GSBlockEventTime startTime,
			GSBlockEventTime endTime) throws GSTimelineDeltaException {
		
		if (!entry.getStartTime().isEqual(startTime) || !entry.getEndTime().isEqual(endTime))
			throw new GSTimelineDeltaException("Entry does not have the expected timespan");
	}
	
	protected void checkEntryType(GSTrackEntry entry, GSETrackEntryType type) throws GSTimelineDeltaException {
		if (entry.getType() != type)
			throw new GSTimelineDeltaException("Entry does not have the expected type");
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
