package com.g4mesoft.captureplayback.common.asset;

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

public class GSAssetHistoryPacket implements GSIPacket {

	private GSAssetHistory history;
	
	public GSAssetHistoryPacket() {
	}

	public GSAssetHistoryPacket(GSIAssetHistory history) {
		this.history = new GSAssetHistory(history);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		history = GSAssetHistory.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSAssetHistory.write(buf, history);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.getAssetManager().onAssetHistoryReceived(history);
	}
}
