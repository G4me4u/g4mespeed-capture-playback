package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetRequestResponsePacket implements GSIPacket {

	private GSEAssetRequestResponse response;

	private UUID assetUUID;
	private GSDecodedAssetFile assetFile;

	public GSAssetRequestResponsePacket() {
	}

	/* request denied */
	public GSAssetRequestResponsePacket(UUID assetUUID) {
		if (assetUUID == null)
			throw new IllegalArgumentException("assetUUID is null");
		response = GSEAssetRequestResponse.DENIED;
	
		this.assetUUID = assetUUID;
		assetFile = null;
	}

	/* request success */
	public GSAssetRequestResponsePacket(GSDecodedAssetFile assetFile) {
		if (assetFile == null)
			throw new IllegalArgumentException("assetFile is null");
		assetUUID = assetFile.getAsset().getUUID();
		response = GSEAssetRequestResponse.SUCCESS;
		this.assetFile = assetFile.copy();
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		response = GSEAssetRequestResponse.fromIndex(buf.readUnsignedByte());
		if (response == null)
			throw new IOException("Unknown response");
		switch (response) {
		case DENIED:
			assetUUID = buf.readUUID();
			break;
		case SUCCESS:
			assetFile = GSDecodedAssetFile.read(buf);
			break;
		}
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUnsignedByte((short)response.getIndex());
		switch (response) {
		case DENIED:
			buf.writeUUID(assetUUID);
			break;
		case SUCCESS:
			GSDecodedAssetFile.write(buf, assetFile);
			break;
		}
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null) {
			GSClientAssetManager assetManager = module.getAssetManager();
			switch (response) {
			case DENIED:
				assetManager.onAssetRequestDenied(assetUUID);
				break;
			case SUCCESS:
				assetManager.onAssetRequestSuccess(assetFile);
				break;
			}
		}
	}
	
	private enum GSEAssetRequestResponse {
		
		DENIED(0),
		SUCCESS(1);

		private final int index;
	
		private GSEAssetRequestResponse(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return index;
		}
		
		private static GSEAssetRequestResponse fromIndex(int index) {
			switch (index) {
			case 0:
				return DENIED;
			case 1:
				return SUCCESS;
			}
			return null;
		}
	}
}
