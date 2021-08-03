package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;

import net.minecraft.network.PacketByteBuf;

public interface GSISessionDelta {

	public void apply(GSSession session) throws GSDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;

	public GSSessionFieldType<?> getType();
	
}
