package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

import net.minecraft.network.PacketByteBuf;

public class GSEntryTypeDelta extends GSEntryDelta {

	private GSETrackEntryType newType;
	private GSETrackEntryType oldType;

	public GSEntryTypeDelta() {
	}
	
	public GSEntryTypeDelta(UUID trackUUID, UUID entryUUID, GSETrackEntryType newType, GSETrackEntryType oldType) {
		super(trackUUID, entryUUID);
		
		this.newType = newType;
		this.oldType = oldType;
	}

	private void setEntryType(GSETrackEntryType newType, GSETrackEntryType oldType, 
			GSTimeline timeline) throws GSTimelineDeltaException {
	
		GSTrackEntry entry = getEntry(timeline);
		checkEntryType(entry, oldType);
		entry.setType(newType);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setEntryType(oldType, newType, timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setEntryType(newType, oldType, timeline);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newType = GSETrackEntryType.fromIndex(buf.readInt());
		oldType = GSETrackEntryType.fromIndex(buf.readInt());
		
		if (newType == null || oldType == null)
			throw new IOException("Invalid type index!");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeInt(newType.getIndex());
		buf.writeInt(oldType.getIndex());
	}
}
