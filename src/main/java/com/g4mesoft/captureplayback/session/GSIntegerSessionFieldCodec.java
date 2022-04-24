package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public class GSIntegerSessionFieldCodec implements GSISessionFieldCodec<Integer> {

	@Override
	public Integer decode(PacketByteBuf buf) throws IOException {
		if (!buf.isReadable(4))
			throw new IOException("Not enough bytes");

		return buf.readInt();
	}

	@Override
	public void encode(PacketByteBuf buf, Integer value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeInt(value.intValue());
	}
}
