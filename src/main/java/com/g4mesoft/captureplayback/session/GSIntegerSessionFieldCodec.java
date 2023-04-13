package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSIntegerSessionFieldCodec implements GSISessionFieldCodec<Integer> {

	@Override
	public Integer decode(GSDecodeBuffer buf) throws IOException {
		if (!buf.isReadable(4))
			throw new IOException("Not enough bytes");

		return buf.readInt();
	}

	@Override
	public void encode(GSEncodeBuffer buf, Integer value) throws IOException {
		if (value == null)
			throw new IOException("value is null");
		
		buf.writeInt(value.intValue());
	}
}
