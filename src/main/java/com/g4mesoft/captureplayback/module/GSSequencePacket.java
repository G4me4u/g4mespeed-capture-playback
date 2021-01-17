package com.g4mesoft.captureplayback.module;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSIPacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSSequencePacket implements GSIPacket {

	private GSSequence sequence;
	
	public GSSequencePacket() {
	}

	public GSSequencePacket(GSSequence sequence) {
		this.sequence = new GSSequence(sequence.getSequenceUUID());
		this.sequence.set(sequence);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		sequence = GSSequence.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSSequence.write(buf, sequence);
	}
	
	@Override
	public void handleOnServer(GSControllerServer controller, ServerPlayerEntity player) {
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handleOnClient(GSControllerClient controller) {
		CapturePlaybackMod.getInstance().getExtension().getClientModule().onSequenceReceived(sequence);
	}
}
