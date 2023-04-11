package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.minecraft.entity.player.PlayerEntity;

public class GSAssetInfo implements Comparable<GSAssetInfo> {

	public static final UUID UNKNOWN_OWNER_UUID = new UUID(0L, 0L);
	
	private final GSEAssetType type;
	private final UUID assetUUID;
	private final GSAssetHandle handle;
	private final GSAssetInfo refInfo;
	
	private String assetName;
	private final long createdTimestamp;
	private long lastModifiedTimestamp;
	private final UUID createdByUUID;
	private UUID ownerUUID;
	private final Set<UUID> permUUIDs;

	public GSAssetInfo(GSEAssetType type, UUID assetUUID, GSAssetHandle handle, String assetName, long createdTimestamp, UUID createdByUUID, UUID ownerUUID) {
		this(type, assetUUID, handle, assetName, createdTimestamp, createdTimestamp, createdByUUID, ownerUUID);
	}

	public GSAssetInfo(GSEAssetType type, UUID assetUUID, GSAssetHandle handle, String assetName, long createdTimestamp, long lastModifiedTimestamp, UUID createdByUUID, UUID ownerUUID) {
		this(type, assetUUID, handle, assetName, createdTimestamp, lastModifiedTimestamp, createdByUUID, ownerUUID, null);
	}
	
	public GSAssetInfo(GSEAssetType type, UUID assetUUID, GSAssetHandle handle, String assetName, long createdTimestamp, long lastModifiedTimestamp, UUID createdByUUID, UUID ownerUUID, Set<UUID> permUUIDs) {
		this.type = type;
		this.assetUUID = assetUUID;
		this.handle = handle;
		refInfo = null;
		
		this.assetName = assetName;
		this.createdTimestamp = createdTimestamp;
		this.lastModifiedTimestamp = lastModifiedTimestamp;
		this.createdByUUID = createdByUUID;
		this.ownerUUID = ownerUUID;
		this.permUUIDs = (permUUIDs != null) ? new LinkedHashSet<>(permUUIDs) : new LinkedHashSet<>();
	}

	public GSAssetInfo(GSEAssetType type, UUID assetUUID, GSAssetInfo refInfo) {
		this.type = type;
		this.assetUUID = assetUUID;
		this.refInfo = refInfo;
		// The following parameters are derived from the
		// reference asset info.
		assetName = null;
		createdTimestamp = 0L;
		lastModifiedTimestamp = 0L;
		createdByUUID = null;
		ownerUUID = null;
		permUUIDs = null;
		// derived assets do not have a handle.
		handle = null;
	}

	public GSAssetInfo(GSAssetInfo other) {
		type = other.type;
		assetUUID = other.assetUUID;
		handle = other.handle;
		refInfo = other.refInfo;
		
		assetName = other.assetName;
		createdTimestamp = other.createdTimestamp;
		lastModifiedTimestamp = other.lastModifiedTimestamp;
		createdByUUID = other.createdByUUID;
		ownerUUID = other.ownerUUID;
		permUUIDs = new LinkedHashSet<>(other.permUUIDs);
	}

	public GSEAssetType getType() {
		return type;
	}
	
	public UUID getAssetUUID() {
		return assetUUID;
	}

	public GSAssetHandle getHandle() {
		return handle;
	}

	public String getAssetName() {
		return isDerived() ? refInfo.getAssetName() : assetName;
	}

	void setAssetName(String assetName) {
		if (isDerived())
			throw new IllegalStateException("Unable to modify derived asset info");
		this.assetName = assetName;
	}
	
	public long getCreatedTimestamp() {
		return isDerived() ? refInfo.getCreatedTimestamp() : createdTimestamp;
	}

	public long getLastModifiedTimestamp() {
		return isDerived() ? refInfo.getLastModifiedTimestamp() : lastModifiedTimestamp;
	}
	
	void setLastModifiedTimestamp(long timestamp) {
		if (isDerived())
			throw new IllegalStateException("Unable to modify derived asset info");
		lastModifiedTimestamp = timestamp;
	}
	
	public UUID getCreatedByUUID() {
		return createdByUUID;
	}
	
	public UUID getOwnerUUID() {
		return isDerived() ? refInfo.getOwnerUUID() : ownerUUID;
	}

	void setOwnerUUID(UUID ownerUUID) {
		if (isDerived())
			throw new IllegalStateException("Unable to modify derived asset info");
		this.ownerUUID = ownerUUID;
	}
	
	public Set<UUID> getPermissionUUIDs() {
		return isDerived() ? refInfo.getPermissionUUIDs() : Collections.unmodifiableSet(permUUIDs);
	}

	void addPermission(UUID playerUUID) {
		if (isDerived())
			throw new IllegalStateException("Unable to modify derived asset info");
		permUUIDs.add(playerUUID);
	}

	void removePermission(UUID playerUUID) {
		if (isDerived())
			throw new IllegalStateException("Unable to modify derived asset info");
		permUUIDs.remove(playerUUID);
	}
	
	public boolean hasPermission(PlayerEntity player) {
		if (player.hasPermissionLevel(GSServerController.OP_PERMISSION_LEVEL)) {
			// OP players have access to all assets.
			return true;
		}
		UUID ownerUUID = getOwnerUUID();
		if (ownerUUID.equals(player.getUuid())) {
			// Direct permission
			return true;
		}
		if (ownerUUID.equals(UNKNOWN_OWNER_UUID)) {
			// Old format assets have unknown owners
			return true;
		}
		// Last more expensive permission check
		return permUUIDs.contains(player.getUuid());
	}
	
	public boolean isDerived() {
		return refInfo != null;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + type.hashCode();
		hash = 31 * hash + assetUUID.hashCode();
		hash = 31 * hash + Objects.hashCode(handle);
		hash = 31 * hash + Long.hashCode(createdTimestamp);
		hash = 31 * hash + Long.hashCode(lastModifiedTimestamp);
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSAssetInfo) {
			GSAssetInfo other = (GSAssetInfo)obj;
			if (type != other.type)
				return false;
			if (!assetUUID.equals(other.assetUUID))
				return false;
			if (!Objects.equals(handle, other.handle))
				return false;
			if (createdTimestamp != other.createdTimestamp)
				return false;
			if (lastModifiedTimestamp != other.lastModifiedTimestamp)
				return false;
			// The remaining parameters are considered as supplementary.
			return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(GSAssetInfo other) {
		// The asset information is sorted in the following order.
		//    - latest last-modified time first
		//    - latest created time first
		//    - lowest asset type index first
		//    - lowest asset handle first
		//    - lowest asset UUID first
		long lastModifiedDelta = lastModifiedTimestamp - other.lastModifiedTimestamp;
		if (lastModifiedDelta != 0L)
			return lastModifiedDelta > 0L ? -1 : 1;
		long createdDelta = createdTimestamp - other.createdTimestamp;
		if (createdDelta != 0L)
			return createdDelta > 0L ? -1 : 1;
		if (type != other.type)
			return type.getIndex() < other.type.getIndex() ? -1 : 1;
		if (!Objects.equals(handle, other.handle)) {
			// Non-null handles first
			if (other.handle == null)
				return -1;
			if (handle == null)
				return 1;
			return handle.compareTo(other.handle);
		}
		// The asset UUID should be unique to each asset.
		return assetUUID.compareTo(other.assetUUID);
	}
	
	public static GSAssetInfo read(GSDecodeBuffer buf) throws IOException {
		GSEAssetType type = GSEAssetType.fromIndex(buf.readUnsignedByte());
		if (type == null)
			throw new IOException("Unknown asset type");
		UUID assetUUID = buf.readUUID();
		GSAssetHandle handle = GSAssetHandle.read(buf);
		String assetName = buf.readString();
		long createdTimestamp = buf.readLong();
		long lastModifiedTimestamp = buf.readLong();
		UUID createdByUUID = buf.readUUID();
		UUID ownerUUID = buf.readUUID();
		int permUUIDCount = buf.readInt();
		GSAssetInfo info = new GSAssetInfo(
			type,
			assetUUID,
			handle,
			assetName,
			createdTimestamp,
			lastModifiedTimestamp,
			createdByUUID,
			ownerUUID
		);
		while (permUUIDCount-- > 0)
			info.addPermission(buf.readUUID());
		return info;
	}
	
	public static void write(GSEncodeBuffer buf, GSAssetInfo info) throws IOException {
		if (info.isDerived())
			throw new IOException("Writing derived asset info is unsupported");
		buf.writeUnsignedByte((short)info.getType().getIndex());
		buf.writeUUID(info.getAssetUUID());
		GSAssetHandle.write(buf, info.getHandle());
		buf.writeString(info.getAssetName());
		buf.writeLong(info.getCreatedTimestamp());
		buf.writeLong(info.getLastModifiedTimestamp());
		buf.writeUUID(info.getCreatedByUUID());
		buf.writeUUID(info.getOwnerUUID());
		Set<UUID> permUUIDs = info.getPermissionUUIDs();
		buf.writeInt(permUUIDs.size());
		for (UUID permUUID : permUUIDs)
			buf.writeUUID(permUUID);
	}
}
