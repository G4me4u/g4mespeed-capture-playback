package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionRequestPacket implements GSIPacket {

	private GSESessionRequestType requestType;
	private UUID assetUUID;
	
	public GSSessionRequestPacket() {
	}

	public GSSessionRequestPacket(GSESessionRequestType requestType, UUID structureUUID) {
		this.requestType = requestType;
		this.assetUUID = structureUUID;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		requestType = GSESessionRequestType.fromIndex(buf.readInt());
		if (requestType == null)
			throw new IOException("Unknown request type");
		assetUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeInt(requestType.getIndex());
		buf.writeUuid(assetUUID);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null)
			module.onSessionRequest(player, requestType, assetUUID);
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
