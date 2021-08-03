package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.panel.GSEContentOpacity;

import net.minecraft.network.PacketByteBuf;

public class GSOpacitySessionFieldCodec implements GSISessionFieldCodec<GSEContentOpacity> {

	@Override
	public GSEContentOpacity decode(PacketByteBuf buf) throws IOException {
		if (!buf.isReadable(1))
			throw new IOException("Not enough bytes");

		GSEContentOpacity opacity = GSEContentOpacity.fromIndex(buf.readByte());
		if (opacity == null)
			throw new IOException("Unknown opacity");
		return opacity;
	}

	@Override
	public void encode(PacketByteBuf buf, GSEContentOpacity value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeByte(value.getIndex() & 0xFF);
	}
}
