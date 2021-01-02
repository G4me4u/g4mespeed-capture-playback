package com.g4mesoft.captureplayback.module;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;

public class GSTimelinePacket implements GSIPacket {

	private GSTimeline timeline;
	
	public GSTimelinePacket() {
	}

	public GSTimelinePacket(GSTimeline timeline) {
		this.timeline = new GSTimeline(timeline.getTimelineUUID());
		this.timeline.set(timeline);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		timeline = GSTimeline.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSTimeline.write(buf, timeline);
	}
	
	@Override
	public void handleOnServer(GSControllerServer controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSControllerClient controller) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().onTimelineReceived(timeline);
	}
}
