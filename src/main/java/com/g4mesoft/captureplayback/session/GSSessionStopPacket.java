package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionStopPacket implements GSIPacket {

	private GSESessionType sessionType;
	
	public GSSessionStopPacket() {
	}

	public GSSessionStopPacket(GSESessionType sessionType) {
		this.sessionType = sessionType;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		sessionType = GSESessionType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown session type");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeInt(sessionType.getIndex());
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.onSessionStop(sessionType);
	}
}
