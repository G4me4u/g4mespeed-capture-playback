package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetInfoRemovedPacket implements GSIPacket {

	private UUID assetUUID;
	
	public GSAssetInfoRemovedPacket() {
	}

	public GSAssetInfoRemovedPacket(UUID assetUUID) {
		this.assetUUID = assetUUID;
	}
	
	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(assetUUID);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		assetUUID = buf.readUuid();
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.onAssetInfoRemoved(assetUUID);
	}
}
