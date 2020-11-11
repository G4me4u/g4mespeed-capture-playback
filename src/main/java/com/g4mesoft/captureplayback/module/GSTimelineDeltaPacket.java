package com.g4mesoft.captureplayback.module;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.timeline.delta.GSITimelineDelta;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSTimelineDeltaPacket implements GSIPacket {

	private GSITimelineDelta delta;
	
	public GSTimelineDeltaPacket() {
	}

	public GSTimelineDeltaPacket(GSITimelineDelta delta) {
		this.delta = delta;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		
		delta = extension.getDeltaRegistry().createNewElement(buf.readInt());
		if (delta == null)
			throw new IOException("Invalid delta ID");
		delta.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		buf.writeInt(extension.getDeltaRegistry().getIdentifier(delta));
		delta.write(buf);
	}
	
	@Override
	public void handleOnServer(GSControllerServer controller, ServerPlayerEntity player) {
		CapturePlaybackMod.getInstance().getExtension().getServerModule().onClientDeltaReceived(delta, player);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSControllerClient controller) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().onServerDeltaReceived(delta);
	}
}
