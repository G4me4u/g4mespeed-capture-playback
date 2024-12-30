package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSAssetFileHeader {

	private static final int VERSIONED_FORMAT_BIT       = 0x80;
	/* First file format version to contain createdByCacheEntry */
	public static final int PLAYER_CACHE_ENTRY_FORMAT_VERSION = 0x01;
	/* Minimum and maximum allowed format version */
	private static final int MIN_FORMAT_VERSION = 0x00;
	private static final int MAX_FORMAT_VERSION = 0x7F;
	/* Latest version of those defined above */
	private static final int LATEST_FORMAT_VERSION = PLAYER_CACHE_ENTRY_FORMAT_VERSION;
	
	private final int formatVersion;
	private final GSEAssetType type;
	private final long createdTimestamp;
	private final UUID createdByUUID;
	private final GSPlayerCacheEntry createdByCacheEntry;

	public GSAssetFileHeader(GSAssetInfo info, GSIPlayerCache playerCache) {
		// Note: unknown type (null) is caught in below constructor.
		this(LATEST_FORMAT_VERSION, info.getType(), info.getCreatedTimestamp(),
				info.getCreatedByUUID(), playerCache.get(info.getCreatedByUUID()));
	}

	private GSAssetFileHeader(int formatVersion, GSEAssetType type,
	                          long createdTimestamp, UUID createdByUUID,
	                          GSPlayerCacheEntry createdByCacheEntry) {
		if (formatVersion < MIN_FORMAT_VERSION || formatVersion > MAX_FORMAT_VERSION)
			throw new IllegalArgumentException("Invalid format version");
		if (type == null)
			throw new IllegalArgumentException("type is null");
		if (createdByUUID == null)
			throw new IllegalArgumentException("createdByUUID is null");
		this.formatVersion = formatVersion;
		this.type = type;
		this.createdTimestamp = createdTimestamp;
		this.createdByUUID = createdByUUID;
		// Note: might be null
		this.createdByCacheEntry = createdByCacheEntry;
	}

	public GSAssetFileHeader(GSAssetFileHeader other) {
		formatVersion = other.formatVersion;
		type = other.type;
		createdTimestamp = other.createdTimestamp;
		createdByUUID = other.createdByUUID;
		createdByCacheEntry = other.createdByCacheEntry;
	}

	public int getFormatVersion() {
		return formatVersion;
	}
	
	public GSEAssetType getType() {
		return type;
	}
	
	public long getCreatedTimestamp() {
		return createdTimestamp;
	}

	public UUID getCreatedByUUID() {
		return createdByUUID;
	}
	
	/* Note: cache entry might be null */
	public GSPlayerCacheEntry getCreatedByCacheEntry() {
		return createdByCacheEntry;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + type.hashCode();
		hash = 31 * hash + Long.hashCode(createdTimestamp);
		hash = 31 * hash + createdByUUID.hashCode();
		// See comment below for note on createdByCacheEntry.
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSAssetFileHeader) {
			GSAssetFileHeader other = (GSAssetFileHeader)obj;
			if (type != other.type)
				return false;
			if (createdTimestamp != other.createdTimestamp)
				return false;
			if (!createdByUUID.equals(other.createdByUUID))
				return false;
			// Note: The player cache entry is assumed equal
			//       if the createdByUUID is equal.
			return true;
		}
		return false;
	}
	
	public static GSAssetFileHeader read(GSDecodeBuffer buf) throws IOException {
		int formatVersion = buf.readUnsignedByte();
		int typeIndex;
		if ((formatVersion & VERSIONED_FORMAT_BIT) != 0) {
			// Previous versions of the format did not have
			// the MSB of the type-index set. This was prior
			// to the first byte containing the file format.
			formatVersion &= ~VERSIONED_FORMAT_BIT;
			if (formatVersion == 0x00)
				throw new IOException("Invalid format version");
			typeIndex = buf.readUnsignedByte();
		} else {
			// The type index is the first byte
			typeIndex = ((int)formatVersion) & 0xFF;
			formatVersion = 0x00;
		}
		GSEAssetType type = GSEAssetType.fromIndex(typeIndex);
		if (type == null)
			throw new IOException("Unknown asset type");
		long createdTimestamp = buf.readLong();
		UUID createdByUUID = buf.readUUID();
		// Format version >=0x01 also has a player cache entry
		// for created by.
		GSPlayerCacheEntry createdByCacheEntry;
		if (formatVersion >= PLAYER_CACHE_ENTRY_FORMAT_VERSION) {
			createdByCacheEntry = buf.readBoolean() ?
					GSPlayerCacheEntry.read(buf) : null;
		} else {
			// Format version < 0x01
			createdByCacheEntry = null;
		}
		return new GSAssetFileHeader(formatVersion, type, createdTimestamp,
				createdByUUID, createdByCacheEntry);
	}
	
	public static void write(GSEncodeBuffer buf, GSAssetFileHeader header) throws IOException {
		// Note: silently upgrade to latest format version
		buf.writeUnsignedByte((short)(VERSIONED_FORMAT_BIT | LATEST_FORMAT_VERSION));
		buf.writeUnsignedByte((short)header.getType().getIndex());
		buf.writeLong(header.getCreatedTimestamp());
		buf.writeUUID(header.getCreatedByUUID());
		buf.writeBoolean(header.getCreatedByCacheEntry() != null);
		if (header.getCreatedByCacheEntry() != null)
			GSPlayerCacheEntry.write(buf, header.getCreatedByCacheEntry());
	}
}
