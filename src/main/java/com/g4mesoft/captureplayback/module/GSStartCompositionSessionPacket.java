package com.g4mesoft.captureplayback.module;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSStartCompositionSessionPacket implements GSIPacket {

	private GSCompositionSession session;
	private GSComposition composition;
	
	public GSStartCompositionSessionPacket() {
	}

	public GSStartCompositionSessionPacket(GSCompositionSession session, GSComposition composition) {
		this.session = session;
		this.composition = new GSComposition(composition);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		session = GSCompositionSession.read(buf);
		composition = GSComposition.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSCompositionSession.write(buf, session);
		GSComposition.write(buf, composition);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().onStartCompositionSession(session, composition);
	}
}
