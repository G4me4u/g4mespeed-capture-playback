package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSAssetHeader {

	private final GSEAssetType type;
	private final long createdTimestamp;
	private final UUID createdByUUID;

	public GSAssetHeader(GSAssetInfo info) {
		this(info.getType(), info.getCreatedTimestamp(), info.getCreatedByUUID());
	}

	public GSAssetHeader(GSEAssetType type, long createdTimestamp, UUID createdByUUID) {
		if (type == null)
			throw new IllegalArgumentException("type is null");
		if (createdByUUID == null)
			throw new IllegalArgumentException("createdByUUID is null");
		this.type = type;
		this.createdTimestamp = createdTimestamp;
		this.createdByUUID = createdByUUID;
	}

	public GSAssetHeader(GSAssetHeader other) {
		type = other.type;
		createdTimestamp = other.createdTimestamp;
		createdByUUID = other.createdByUUID;
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

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + type.hashCode();
		hash = 31 * hash + Long.hashCode(createdTimestamp);
		hash = 31 * hash + createdByUUID.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSAssetHeader) {
			GSAssetHeader other = (GSAssetHeader)obj;
			if (type != other.type)
				return false;
			if (createdTimestamp != other.createdTimestamp)
				return false;
			if (!createdByUUID.equals(other.createdByUUID))
				return false;
			return true;
		}
		return false;
	}
	
	public static GSAssetHeader read(GSDecodeBuffer buf) throws IOException {
		GSEAssetType type = GSEAssetType.fromIndex(buf.readUnsignedByte());
		if (type == null)
			throw new IOException("Unknown asset type");
		long createdTimestamp = buf.readLong();
		UUID createdByUUID = buf.readUUID();
		return new GSAssetHeader(type, createdTimestamp, createdByUUID);
	}
	
	public static void write(GSEncodeBuffer buf, GSAssetHeader header) throws IOException {
		buf.writeUnsignedByte((short)header.getType().getIndex());
		buf.writeLong(header.getCreatedTimestamp());
		buf.writeUUID(header.getCreatedByUUID());
	}
}
