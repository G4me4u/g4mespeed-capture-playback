package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryRemovedDelta extends GSEntryAddedDelta {

	private GSETrackEntryType type;
	
	public GSEntryRemovedDelta() {
	}

	public GSEntryRemovedDelta(GSTrackEntry entry) {
		this(entry.getOwnerTrack().getTrackUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime(), entry.getType());
	}
	
	public GSEntryRemovedDelta(UUID trackUUID, UUID entryUUID, GSBlockEventTime startTime,
			GSBlockEventTime endTime, GSETrackEntryType type) {
		
		super(trackUUID, entryUUID, startTime, endTime);
		
		this.type = type;
	}

	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		super.applyDelta(timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		super.unapplyDelta(timeline);
	}
	
	@Override
	protected GSETrackEntryType getExpectedType() {
		return type;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		type = GSETrackEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Type index invalid");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeInt(type.getIndex());
	}
}
