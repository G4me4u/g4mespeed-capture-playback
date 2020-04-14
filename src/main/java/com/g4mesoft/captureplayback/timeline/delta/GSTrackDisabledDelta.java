package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;

import net.minecraft.util.PacketByteBuf;

public class GSTrackDisabledDelta extends GSTrackDelta {

	private boolean newDisabled;
	private boolean oldDisabled;

	public GSTrackDisabledDelta() {
	}
	
	public GSTrackDisabledDelta(UUID trackUUID, boolean newDisabled, boolean oldDisabled) {
		super(trackUUID);
		
		this.newDisabled = newDisabled;
		this.oldDisabled = oldDisabled;
	}

	private void setTrackDisabled(boolean newDisabled, boolean oldDisabled, GSTimeline timeline) throws GSTimelineDeltaException {
		GSTrack track = getTrack(timeline);
		checkTrackDisabled(track, oldDisabled);
		track.setDisabled(newDisabled);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setTrackDisabled(oldDisabled, newDisabled, timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setTrackDisabled(newDisabled, oldDisabled, timeline);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newDisabled = buf.readBoolean();
		oldDisabled = buf.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeBoolean(newDisabled);
		buf.writeBoolean(newDisabled);
	}
}
