package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public interface GSISessionFieldCodec<T> {

	public T decode(GSDecodeBuffer buf) throws IOException;

	public void encode(GSEncodeBuffer buf, T value) throws IOException;
	
}
