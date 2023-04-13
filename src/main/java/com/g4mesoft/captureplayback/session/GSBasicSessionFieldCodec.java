package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;
import com.g4mesoft.util.GSFileUtil;

public class GSBasicSessionFieldCodec<T> implements GSISessionFieldCodec<T> {

	private final GSFileUtil.GSFileDecoder<T> decoder;
	private final GSFileUtil.GSFileEncoder<T> encoder;
	
	public GSBasicSessionFieldCodec(GSFileUtil.GSFileDecoder<T> decoder, GSFileUtil.GSFileEncoder<T> encoder) {
		this.decoder = decoder;
		this.encoder = encoder;
	}
	
	@Override
	public T decode(GSDecodeBuffer buf) throws IOException {
		if (buf.readBoolean()) {
			try {
				return decoder.decode(buf);
			} catch (Exception e) {
				throw new IOException("Unable to decode field", e);
			}
		}
		
		return null;
	}
	
	@Override
	public void encode(GSEncodeBuffer buf, T value) throws IOException {
		buf.writeBoolean(value != null);
		
		if (value != null) {
			try {
				encoder.encode(buf, value);
			} catch (Exception e) {
				throw new IOException("Unable to encode field", e);
			}
		}
	}
}
