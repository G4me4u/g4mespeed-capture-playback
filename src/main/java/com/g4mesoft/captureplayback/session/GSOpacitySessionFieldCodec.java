package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.panel.GSEContentOpacity;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSOpacitySessionFieldCodec implements GSISessionFieldCodec<GSEContentOpacity> {

	@Override
	public GSEContentOpacity decode(GSDecodeBuffer buf) throws IOException {
		if (!buf.isReadable(1))
			throw new IOException("Not enough bytes");

		GSEContentOpacity opacity = GSEContentOpacity.fromIndex(buf.readUnsignedByte());
		if (opacity == null)
			throw new IOException("Unknown opacity");
		return opacity;
	}

	@Override
	public void encode(GSEncodeBuffer buf, GSEContentOpacity value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeUnsignedByte((short)value.getIndex());
	}
}
