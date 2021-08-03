package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public interface GSISessionFieldCodec<T> {

	public T decode(PacketByteBuf buf) throws IOException;

	public void encode(PacketByteBuf buf, T value) throws IOException;
	
}
