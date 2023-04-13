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

public class GSAssetInfoChangedPacket implements GSIPacket {

	private GSAssetInfo info;
	
	public GSAssetInfoChangedPacket() {
	}

	public GSAssetInfoChangedPacket(GSAssetInfo info) {
		this.info = new GSAssetInfo(info);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		info = GSAssetInfo.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		GSAssetInfo.write(buf, info);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.getAssetManager().onAssetInfoChanged(info);
	}
}
