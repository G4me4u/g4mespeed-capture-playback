package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import net.minecraft.network.PacketByteBuf;

public class GSDoubleSessionFieldCodec implements GSISessionFieldCodec<Double> {

	@Override
	public Double decode(PacketByteBuf buf) throws IOException {
		if (!buf.isReadable(4))
			throw new IOException("Not enough bytes");

		return buf.readDouble();
	}

	@Override
	public void encode(PacketByteBuf buf, Double value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeDouble(value.doubleValue());
	}
}
