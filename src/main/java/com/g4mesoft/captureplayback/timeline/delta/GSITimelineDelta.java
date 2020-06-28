package com.g4mesoft.captureplayback.timeline.delta;

import java.io.IOException;

import com.g4mesoft.captureplayback.timeline.GSTimeline;

import net.minecraft.network.PacketByteBuf;

public interface GSITimelineDelta {

	public void unapplyDelta(GSTimeline timeline) throws GSTimelineDeltaException;
	
	public void applyDelta(GSTimeline timeline) throws GSTimelineDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;
	
}
