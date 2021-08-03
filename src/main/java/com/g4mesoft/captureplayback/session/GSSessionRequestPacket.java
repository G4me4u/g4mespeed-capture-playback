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

	private GSESessionType sessionType;
	private GSESessionRequestType requestType;
	private UUID structureUUID;
	
	public GSSessionRequestPacket() {
	}

	public GSSessionRequestPacket(GSESessionType sessionType, GSESessionRequestType requestType, UUID structureUUID) {
		this.sessionType = sessionType;
		this.requestType = requestType;
		this.structureUUID = structureUUID;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		sessionType = GSESessionType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown session type");
		requestType = GSESessionRequestType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown request type");
		structureUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeInt(sessionType.getIndex());
		buf.writeInt(requestType.getIndex());
		buf.writeUuid(structureUUID);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null)
			module.onSessionRequest(player, sessionType, requestType, structureUUID);
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
