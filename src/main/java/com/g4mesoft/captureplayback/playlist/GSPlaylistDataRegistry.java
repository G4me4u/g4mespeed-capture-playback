package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;
import com.g4mesoft.util.GSFileUtil.GSFileDecoder;
import com.g4mesoft.util.GSFileUtil.GSFileEncoder;

public final class GSPlaylistDataRegistry {

	private static final Map<Class<? extends GSIPlaylistData>, Integer> identifierRegistry;
	private static final Map<Integer, GSFileDecoder<? extends GSIPlaylistData>> decoderRegistry;
	private static final Map<Integer, GSFileEncoder<? extends GSIPlaylistData>> encoderRegistry;
	
	static {
		identifierRegistry = new IdentityHashMap<>();
		decoderRegistry = new HashMap<>();
		encoderRegistry = new HashMap<>();
		
		register(0, GSUnspecifiedPlaylistData.class, GSUnspecifiedPlaylistData::read, GSUnspecifiedPlaylistData::write);
		register(1, GSHotkeyPlaylistData.class, GSHotkeyPlaylistData::read, GSHotkeyPlaylistData::write);
		register(2, GSAssetPlaylistData.class, GSAssetPlaylistData::read, GSAssetPlaylistData::write);
		register(3, GSDelayPlaylistData.class, GSDelayPlaylistData::read, GSDelayPlaylistData::write);
	}
	
	private GSPlaylistDataRegistry() {
	}
	
	private static <T extends GSIPlaylistData> void register(int identifier, Class<T> dataClazz, GSFileDecoder<T> decoder, GSFileEncoder<T> encoder) {
		identifierRegistry.put(dataClazz, identifier);
		decoderRegistry.put(identifier, decoder);
		encoderRegistry.put(identifier, encoder);
	}
	
	public static int getIdentifier(Class<? extends GSIPlaylistData> dataClazz) {
		Integer identifier = identifierRegistry.get(dataClazz);
		if (identifier == null)
			throw new RuntimeException("Missing registry entry for dataClazz: " + dataClazz.getSimpleName());
		return identifier.intValue();
	}

	public static GSFileDecoder<? extends GSIPlaylistData> getDecoder(int identifier) {
		return decoderRegistry.get(identifier);
	}

	public static GSFileEncoder<? extends GSIPlaylistData> getEncoder(int identifier) {
		return encoderRegistry.get(identifier);
	}
	
	public static GSIPlaylistData readData(GSDecodeBuffer buf) throws IOException {
		int identifier = buf.readInt();
		GSFileDecoder<? extends GSIPlaylistData> decoder = GSPlaylistDataRegistry.getDecoder(identifier);
		if (decoder == null)
			throw new IOException("Corrupted playlist data");
		GSIPlaylistData data;
		try {
			data = decoder.decode(buf);
		} catch (Throwable throwable) {
			throw new IOException("Corrupted playlist data", throwable);
		}
		return data;
	}
	
	public static <T extends GSIPlaylistData> void writeData(GSEncodeBuffer buf, T data) throws IOException {
		int identifier = GSPlaylistDataRegistry.getIdentifier(data.getClass());
		buf.writeInt(identifier);
		@SuppressWarnings("unchecked")
		GSFileEncoder<T> encoder = (GSFileEncoder<T>)GSPlaylistDataRegistry.getEncoder(identifier);
		try {
			encoder.encode(buf, data);
		} catch (Throwable throwable) {
			throw new IOException("Unable to write playlist data", throwable);
		}
	}
}
