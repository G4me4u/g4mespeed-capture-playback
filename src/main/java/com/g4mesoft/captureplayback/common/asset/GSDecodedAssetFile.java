package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;
import com.g4mesoft.util.GSFileUtil.GSFileEncoder;

public class GSDecodedAssetFile {

	private final GSAssetFileHeader header;
	private final GSAbstractAsset asset;
	
	public GSDecodedAssetFile(GSAssetFileHeader header, GSAbstractAsset asset) {
		// Note: intentional null-pointer exception
		if (header.getType() != asset.getType())
			throw new IllegalArgumentException("Header type does not match asset");
		this.header = header;
		this.asset = asset;
	}
	
	/* Copies the referenced asset and takes ownership */
	public GSDecodedAssetFile copy() {
		return new GSDecodedAssetFile(header, asset.copy());
	}
	
	public GSAssetFileHeader getHeader() {
		return header;
	}
	
	public GSAbstractAsset getAsset() {
		return asset;
	}
	
	public static GSDecodedAssetFile read(GSDecodeBuffer buf) throws IOException {
		GSAssetFileHeader header = GSAssetFileHeader.read(buf);
		GSAbstractAsset asset;
		try {
			asset = GSAssetRegistry.getDecoder(header.getType()).decode(buf);
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new IOException(e);
		}
		return new GSDecodedAssetFile(header, asset);
	}

	public static void write(GSEncodeBuffer buf, GSDecodedAssetFile assetFile) throws IOException {
		GSAssetFileHeader.write(buf, assetFile.getHeader());
		writeAsset(buf, assetFile.getAsset());
	}
	
	private static <T extends GSAbstractAsset> void writeAsset(GSEncodeBuffer buf, T asset) throws IOException {
		@SuppressWarnings("unchecked")
		GSFileEncoder<T> encoder = (GSFileEncoder<T>)GSAssetRegistry.getEncoder(asset.getType());
		try {
			encoder.encode(buf, asset);
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
