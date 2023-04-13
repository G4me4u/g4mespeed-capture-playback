package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSFloatSessionFieldCodec implements GSISessionFieldCodec<Float> {

	@Override
	public Float decode(GSDecodeBuffer buf) throws IOException {
		if (!buf.isReadable(4))
			throw new IOException("Not enough bytes");

		return buf.readFloat();
	}

	@Override
	public void encode(GSEncodeBuffer buf, Float value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeFloat(value.floatValue());
	}
}
