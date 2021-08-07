package com.g4mesoft.captureplayback.common;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public interface GSIDelta<M> {

	public void apply(M model) throws GSDeltaException;
	
	public void unapply(M model) throws GSDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;
	
}
