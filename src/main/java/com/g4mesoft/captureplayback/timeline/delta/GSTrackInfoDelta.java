package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

import net.minecraft.util.PacketByteBuf;

public class GSTrackInfoDelta extends GSTrackDelta {

	private GSTrackInfo newInfo;
	private GSTrackInfo oldInfo;

	public GSTrackInfoDelta() {
	}

	public GSTrackInfoDelta(UUID trackUUID, GSTrackInfo newInfo, GSTrackInfo oldInfo) {
		super(trackUUID);
		
		this.newInfo = newInfo;
		this.oldInfo = oldInfo;
	}
	
	private void setTrackInfo(GSTrackInfo newInfo, GSTrackInfo oldInfo, GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = getTrack(timeline);
		checkTrackInfo(track, oldInfo);
		track.setInfo(newInfo);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setTrackInfo(oldInfo, newInfo, timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setTrackInfo(newInfo, oldInfo, timeline);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newInfo = GSTrackInfo.read(buf);
		oldInfo = GSTrackInfo.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSTrackInfo.write(buf, newInfo);
		GSTrackInfo.write(buf, oldInfo);
	}
}
