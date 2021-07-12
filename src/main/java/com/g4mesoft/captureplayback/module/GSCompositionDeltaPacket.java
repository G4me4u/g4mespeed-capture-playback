package com.g4mesoft.captureplayback.module;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCompositionDeltaPacket implements GSIPacket {

	private UUID compositionUUID;
	private GSICompositionDelta delta;
	
	public GSCompositionDeltaPacket() {
	}

	public GSCompositionDeltaPacket(UUID compositionUUID, GSICompositionDelta delta) {
		this.compositionUUID = compositionUUID;
		this.delta = delta;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		compositionUUID = buf.readUuid();
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		
		delta = extension.getCompositionDeltaRegistry().createNewElement(buf.readInt());
		if (delta == null)
			throw new IOException("Invalid delta ID");
		delta.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(compositionUUID);
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		buf.writeInt(extension.getCompositionDeltaRegistry().getIdentifier(delta));
		delta.write(buf);
	}
	
	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		CapturePlaybackMod.getInstance().getExtension().getServerModule().onDeltaReceived(delta, player);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().onDeltaReceived(compositionUUID, delta);
	}
}
