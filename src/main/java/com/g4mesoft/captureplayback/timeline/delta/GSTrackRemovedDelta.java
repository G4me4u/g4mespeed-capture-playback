package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

import net.minecraft.util.PacketByteBuf;

public class GSTrackRemovedDelta extends GSTrackDelta {

	private GSTrackInfo info;
	private boolean disabled;
	private GSEntryRemovedDelta[] entryDeltas;

	public GSTrackRemovedDelta() {
	}

	public GSTrackRemovedDelta(GSTrack track) {
		this(track.getTrackUUID(), track.getInfo(), track.isDisabled(), track.getEntries());
	}
	
	public GSTrackRemovedDelta(UUID trackUUID, GSTrackInfo info, boolean disabled, Collection<GSTrackEntry> entries) {
		super(trackUUID);
		
		this.info = info;
		this.disabled = disabled;
		
		entryDeltas = new GSEntryRemovedDelta[entries.size()];

		int i = 0;
		for (GSTrackEntry entry : entries)
			entryDeltas[i++] = new GSEntryRemovedDelta(entry);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = addTrack(timeline, info);
		
		track.setDisabled(disabled);
		
		for (GSEntryDelta entryDelta : entryDeltas)
			entryDelta.unapplyDelta(timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		removeTrack(timeline, info, disabled, entryDeltas.length);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSTrackInfo.read(buf);
		disabled = buf.readBoolean();
		
		entryDeltas = new GSEntryRemovedDelta[buf.readInt()];
		for (int i = 0; i < entryDeltas.length; i++)
			(entryDeltas[i] = new GSEntryRemovedDelta()).read(buf);
	}
	
	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
	
		GSTrackInfo.write(buf, info);
		buf.writeBoolean(disabled);
		
		buf.writeInt(entryDeltas.length);
		for (GSEntryRemovedDelta entryDelta : entryDeltas)
			entryDelta.write(buf);
	}
}
