package com.g4mesoft.captureplayback.module;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSequenceSessionRequestPacket implements GSIPacket {

	private UUID trackUUID;
	
	public GSSequenceSessionRequestPacket() {
	}

	public GSSequenceSessionRequestPacket(UUID trackUUID) {
		this.trackUUID = trackUUID;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		trackUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(trackUUID);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		CapturePlaybackMod.getInstance().getExtension().getServerModule().onSequenceSessionRequest(trackUUID, player);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
