package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryRemovedDelta extends GSEntryDelta {

	private GSSignalTime startTime;
	private GSSignalTime endTime;
	private GSETrackEntryType type;
	
	public GSEntryRemovedDelta() {
	}

	public GSEntryRemovedDelta(GSTrackEntry entry) {
		this(entry.getParent().getTrackUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime(), entry.getType());
	}
	
	public GSEntryRemovedDelta(UUID trackUUID, UUID entryUUID, GSSignalTime startTime,
			GSSignalTime endTime, GSETrackEntryType type) {
		
		super(trackUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
		this.type = type;
	}

	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrackEntry entry = addEntry(timeline, entryUUID, startTime, endTime);
		
		entry.setType(type);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		removeEntry(timeline, entryUUID, startTime, endTime, type);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		startTime = GSSignalTime.read(buf);
		endTime = GSSignalTime.read(buf);
		
		type = GSETrackEntryType.fromIndex(buf.readInt());
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
