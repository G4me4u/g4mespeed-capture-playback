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

public class GSCreateAssetPacket implements GSIPacket {

	private String name;
	private GSEAssetType type;
	private GSAssetHandle handle;
	
	private UUID originalAssetUUID;
	
	public GSCreateAssetPacket() {
	}

	public GSCreateAssetPacket(String name, GSEAssetType type, GSAssetHandle handle, UUID originalAssetUUID) {
		this.name = name;
		this.type = type;
		this.handle = handle;
		// Create duplicate of:
		this.originalAssetUUID = originalAssetUUID;
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		name = buf.readString();
		type = GSEAssetType.fromIndex(buf.readUnsignedByte());
		if (type == null)
			throw new IOException("Unknown asset type");
		handle = GSAssetHandle.read(buf);
		originalAssetUUID = buf.readBoolean() ? buf.readUUID() : null;
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeString(name);
		buf.writeUnsignedByte((short)type.getIndex());
		GSAssetHandle.write(buf, handle);
		buf.writeBoolean(originalAssetUUID != null);
		if (originalAssetUUID != null)
			buf.writeUUID(originalAssetUUID);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null) {
			GSAssetManager assetManager = module.getAssetManager();
			if (originalAssetUUID != null) {
				GSAssetInfo originalInfo = assetManager.getInfo(originalAssetUUID);
				if (originalInfo != null && type == originalInfo.getType() && originalInfo.hasPermission(player))
					assetManager.createDuplicateAsset(handle, name, player.getUuid(), originalAssetUUID);
			} else {
				assetManager.createAsset(type, handle, name, player.getUuid());
			}
		}
		// TODO: send feedback for asset creation
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}