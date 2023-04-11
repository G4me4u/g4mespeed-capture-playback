package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionStartPacket implements GSIPacket {

	private GSSession session;
	
	public GSSessionStartPacket() {
	}

	public GSSessionStartPacket(GSSession session) {
		this.session = session;
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		session = GSSession.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSSession.writePacket(buf, session);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.getAssetManager().onSessionStart(session);
	}
}
