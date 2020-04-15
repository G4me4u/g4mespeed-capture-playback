package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

import net.minecraft.util.PacketByteBuf;

public class GSTrackAddedDelta extends GSTrackDelta {

	private GSTrackInfo info;

	public GSTrackAddedDelta() {
	}
	
	public GSTrackAddedDelta(UUID trackUUID, GSTrackInfo info) {
		super(trackUUID);
		
		this.info = info;
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = getTrack(timeline);
		checkTrackInfo(track, info);
		checkTrackEntryCount(track, 0);
		timeline.removeTrack(trackUUID);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		if (timeline.hasTrackUUID(trackUUID))
			throw new GSTimelineDeltaException("Track already exists");
		
		timeline.addTrack(trackUUID, info);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSTrackInfo.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSTrackInfo.write(buf, info);
	}
}
