package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSRequestAssetPacket implements GSIPacket {

	private UUID assetUUID;
	
	public GSRequestAssetPacket() {
	}

	public GSRequestAssetPacket(UUID assetUUID) {
		if (assetUUID == null)
			throw new IllegalArgumentException("assetUUID is null");
		this.assetUUID = assetUUID;
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		assetUUID = buf.readUUID();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUUID(assetUUID);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		
		GSIPacket packet = null;
		if (module != null) {
			GSAssetManager assetManager = module.getAssetManager();
			GSAssetInfo info = assetManager.getInfo(assetUUID);
			if (info != null && info.hasPermission(player)) {
				GSAssetRef ref = assetManager.requestAsset(assetUUID);
				if (ref != null) {
					GSAssetFileHeader header = new GSAssetFileHeader(info);
					GSDecodedAssetFile assetFile = new GSDecodedAssetFile(header, ref.get());
					packet = new GSAssetRequestResponsePacket(assetFile);
					ref.release();
				}
			}
		}
		
		if (packet != null) {
			controller.sendPacket(packet, player);
		} else {
			// The request was denied...
			controller.sendPacket(new GSAssetRequestResponsePacket(assetUUID), player);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
