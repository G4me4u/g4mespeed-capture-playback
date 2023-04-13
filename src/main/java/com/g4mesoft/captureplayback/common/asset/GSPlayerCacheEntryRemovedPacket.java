package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSPlayerCacheEntryRemovedPacket implements GSIPacket {

	private UUID playerUUID;
	
	public GSPlayerCacheEntryRemovedPacket() {
	}

	public GSPlayerCacheEntryRemovedPacket(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		playerUUID = buf.readUUID();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUUID(playerUUID);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.getAssetManager().getPlayerCache().remove(playerUUID);
	}
}
