package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;

import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSImportAssetPacket implements GSIPacket {

	private String name;
	private GSAssetHandle handle;
	private GSDecodedAssetFile assetFile;
	
	public GSImportAssetPacket() {
	}

	public GSImportAssetPacket(String name, GSAssetHandle handle, GSDecodedAssetFile assetFile) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		if (handle == null)
			throw new IllegalArgumentException("handle is null");
		if (assetFile == null)
			throw new IllegalArgumentException("assetFile is null");
		this.name = name;
		this.handle = handle;
		this.assetFile = assetFile;
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		name = buf.readString();
		handle = GSAssetHandle.read(buf);
		assetFile = GSDecodedAssetFile.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeString(name);
		GSAssetHandle.write(buf, handle);
		GSDecodedAssetFile.write(buf, assetFile);
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null)
			module.getAssetManager().importAsset(handle, name, player.getUuid(), assetFile);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
	}
}
