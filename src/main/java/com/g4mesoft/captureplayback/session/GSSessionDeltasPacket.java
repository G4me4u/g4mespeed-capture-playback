package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.Arrays;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.registry.GSSupplierRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionDeltasPacket implements GSIPacket {

	private GSESessionType sessionType;
	private GSISessionDelta[] deltas;
	
	public GSSessionDeltasPacket() {
	}

	public GSSessionDeltasPacket(GSESessionType sessionType, GSISessionDelta[] deltas) {
		this.sessionType = sessionType;
		this.deltas = deltas;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		sessionType = GSESessionType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown session type");
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSSupplierRegistry<Integer, GSISessionDelta> registry = extension.getSessionDeltaRegistry();
		
		int deltaCount = buf.readInt();
		deltas = new GSISessionDelta[deltaCount];
		
		int i = 0;
		while (deltaCount-- != 0) {
			GSISessionDelta delta = registry.createNewElement(buf.readInt());
			if (delta != null) {
				delta.read(buf);
				deltas[i++] = delta;
			}
		}
		
		if (i == 0)
			throw new IOException("Unable to decode deltas");
		if (i != deltas.length)
			deltas = Arrays.copyOf(deltas, i);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeInt(sessionType.getIndex());
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		GSSupplierRegistry<Integer, GSISessionDelta> registry = extension.getSessionDeltaRegistry();
		
		buf.writeInt(deltas.length);
		for (GSISessionDelta delta : deltas) {
			buf.writeInt(registry.getIdentifier(delta));
			delta.write(buf);
		}
	}

	@Override
	public void handleOnServer(GSServerController controller, ServerPlayerEntity player) {
		GSCapturePlaybackServerModule module = controller.getModule(GSCapturePlaybackServerModule.class);
		if (module != null)
			module.onSessionDeltasReceived(player, sessionType, deltas);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSClientController controller) {
		GSCapturePlaybackClientModule module = controller.getModule(GSCapturePlaybackClientModule.class);
		if (module != null)
			module.onSessionDeltasReceived(sessionType, deltas);
	}
}
