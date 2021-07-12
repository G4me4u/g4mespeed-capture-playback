package com.g4mesoft.captureplayback.module;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSequenceSessionChangedPacket implements GSIPacket {

	private GSSequenceSession session;
	
	public GSSequenceSessionChangedPacket() {
	}

	public GSSequenceSessionChangedPacket(GSSequenceSession session) {
		this.session = session;
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		session = GSSequenceSession.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSSequenceSession.write(buf, session);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		CapturePlaybackMod.getInstance().getExtension().getServerModule().onSequenceSessionChanged(session, player);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
