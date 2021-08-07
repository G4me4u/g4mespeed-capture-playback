package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.Arrays;

import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionDeltasPacket implements GSIPacket {

	private GSESessionType sessionType;
	private GSIDelta<GSSession>[] deltas;
	
	public GSSessionDeltasPacket() {
	}

	public GSSessionDeltasPacket(GSESessionType sessionType, GSIDelta<GSSession>[] deltas) {
		this.sessionType = sessionType;
		this.deltas = deltas;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		sessionType = GSESessionType.fromIndex(buf.readInt());
		if (sessionType == null)
			throw new IOException("Unknown session type");
		
		int deltaCount = buf.readInt();
		@SuppressWarnings("unchecked")
		GSIDelta<GSSession>[] deltas = new GSIDelta[deltaCount];
		
		int i = 0;
		while (deltaCount-- != 0) {
			GSIDelta<GSSession> delta;
			try {
				delta = GSDeltaRegistries.SESSION_DELTA_REGISTRY.read(buf);
			} catch (IOException ignore) {
				continue;
			}
			deltas[i++] = delta;
		}
		
		if (i == 0)
			throw new IOException("Unable to decode deltas");
		if (i != deltas.length)
			deltas = Arrays.copyOf(deltas, i);
	
		this.deltas = deltas;
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeInt(sessionType.getIndex());
	
		buf.writeInt(deltas.length);
		for (GSIDelta<GSSession> delta : deltas)
			GSDeltaRegistries.SESSION_DELTA_REGISTRY.write(buf, delta);
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
