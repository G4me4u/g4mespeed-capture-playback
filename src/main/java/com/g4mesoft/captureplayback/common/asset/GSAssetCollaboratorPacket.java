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

public class GSAssetCollaboratorPacket implements GSIPacket {

	private UUID assetUUID;
	private UUID collabUUID;
	private boolean removed;
	
	public GSAssetCollaboratorPacket() {
	}

	public GSAssetCollaboratorPacket(UUID assetUUID, UUID collabUUID, boolean removed) {
		this.assetUUID = assetUUID;
		this.collabUUID = collabUUID;
		this.removed = removed;
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		assetUUID = buf.readUUID();
		collabUUID = buf.readUUID();
		removed = buf.readBoolean();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUUID(assetUUID);
		buf.writeUUID(collabUUID);
		buf.writeBoolean(removed);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null) {
			GSAssetManager assetManager = module.getAssetManager();
			GSAssetInfo info = assetManager.getStoredHistory().get(assetUUID);
			if (info != null && info.hasExtendedPermission(player)) {
				if (removed) {
					assetManager.removeCollaborator(assetUUID, collabUUID);
				} else {
					assetManager.addCollaborator(assetUUID, collabUUID);
				}
			}
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
