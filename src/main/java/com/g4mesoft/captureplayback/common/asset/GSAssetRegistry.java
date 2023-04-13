package com.g4mesoft.captureplayback.common.asset;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import com.g4mesoft.util.GSFileUtil.GSFileDecoder;
import com.g4mesoft.util.GSFileUtil.GSFileEncoder;

public final class GSAssetRegistry {

	private static final Map<GSEAssetType, GSFileDecoder<? extends GSAbstractAsset>> decoderRegistry;
	private static final Map<GSEAssetType, GSFileEncoder<? extends GSAbstractAsset>> encoderRegistry;
	private static final Map<GSEAssetType, Function<GSAssetInfo, ? extends GSAbstractAsset>> constrRegistry;
	
	static {
		decoderRegistry = new EnumMap<>(GSEAssetType.class);
		encoderRegistry = new EnumMap<>(GSEAssetType.class);
		constrRegistry = new EnumMap<>(GSEAssetType.class);
		
		register(GSEAssetType.COMPOSITION, GSCompositionAsset::read, GSCompositionAsset::write, GSCompositionAsset::new);
		register(GSEAssetType.SEQUENCE, GSSequenceAsset::read, GSSequenceAsset::write, GSSequenceAsset::new);
		register(GSEAssetType.PLAYLIST, GSPlaylistAsset::read, GSPlaylistAsset::write, GSPlaylistAsset::new);
	}
	
	private GSAssetRegistry() {
	}
	
	private static <T extends GSAbstractAsset> void register(GSEAssetType type, GSFileDecoder<T> decoder, GSFileEncoder<T> encoder, Function<GSAssetInfo, T> constr) {
		decoderRegistry.put(type, decoder);
		encoderRegistry.put(type, encoder);
		constrRegistry.put(type, constr);
	}
	
	public static GSFileDecoder<? extends GSAbstractAsset> getDecoder(GSEAssetType assetType) {
		return decoderRegistry.get(assetType);
	}

	public static GSFileEncoder<? extends GSAbstractAsset> getEncoder(GSEAssetType assetType) {
		return encoderRegistry.get(assetType);
	}

	public static Function<GSAssetInfo, ? extends GSAbstractAsset> getConstr(GSEAssetType assetType) {
		return constrRegistry.get(assetType);
	}
}
