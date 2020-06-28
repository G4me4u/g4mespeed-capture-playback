package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSTimelineNameDelta implements GSITimelineDelta {

	private String newName;
	private String oldName;

	public GSTimelineNameDelta() {
	}
	
	public GSTimelineNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSTimeline timeline) throws GSTimelineDeltaException {
		if (!Objects.equals(oldName, timeline.getName()))
			throw new GSTimelineDeltaException("Timeline does not have the expected name");
		timeline.setName(newName);
	}
	
	@Override
	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setName(oldName, newName, timeline);
	}

	@Override
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException {
		setName(newName, oldName, timeline);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		newName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		oldName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
