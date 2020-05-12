package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

import net.minecraft.util.PacketByteBuf;

public abstract class GSTrackDelta implements GSITimelineDelta {

	protected UUID trackUUID;

	protected GSTrackDelta() {
	}
	
	protected GSTrackDelta(UUID trackUUID) {
		this.trackUUID = trackUUID;
	}
	
	protected GSTrack getTrack(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = timeline.getTrack(trackUUID);
		if (track == null)
			throw new GSTimelineDeltaException("Expected track does not exist");
		return track;
	}
	
	protected void checkTrackInfo(GSTrack track, GSTrackInfo info) throws GSTimelineDeltaException {
		if (!track.getInfo().equals(info))
			throw new GSTimelineDeltaException("Track does not have the expected info");
	}
	
	protected void checkTrackDisabled(GSTrack track, boolean disabled) throws GSTimelineDeltaException {
		if (track == null || track.isDisabled() != disabled)
			throw new GSTimelineDeltaException("Track does not have the expected disabled state");
	}
	
	protected void checkTrackEntryCount(GSTrack track, int expectedCount) throws GSTimelineDeltaException {
		if (track.getEntries().size() != expectedCount)
			throw new GSTimelineDeltaException("Track does not have the expected entry count");
	}
	
	protected void removeTrack(GSTimeline timeline, GSTrackInfo info, boolean expectedDisabled, 
			int expectedEntryCount) throws GSTimelineDeltaException {
		
		GSTrack track = getTrack(timeline);
		checkTrackInfo(track, info);
		checkTrackDisabled(track, expectedDisabled);
		checkTrackEntryCount(track, expectedEntryCount);
		timeline.removeTrack(trackUUID);
	}
	
	protected GSTrack addTrack(GSTimeline timeline, GSTrackInfo info) throws GSTimelineDeltaException {
		if (timeline.hasTrackUUID(trackUUID))
			throw new GSTimelineDeltaException("Track already exists");
		
		return timeline.addTrack(trackUUID, info);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		trackUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(trackUUID);
	}
}
