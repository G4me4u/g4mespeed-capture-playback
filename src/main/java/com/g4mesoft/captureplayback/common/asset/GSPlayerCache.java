package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlayerCache extends GSAbstractPlayerCache {

	private final Map<UUID, GSPlayerCacheEntry> entries;
	
	public GSPlayerCache() {
		entries = new HashMap<>();
	}

	public GSPlayerCache(GSIPlayerCache other) {
		this();
		
		set(other);
	}

	/* Internal constructor used for #read(...) */
	private GSPlayerCache(Map<UUID, GSPlayerCacheEntry> entries) {
		this.entries = entries;
	}
	
	@Override
	public GSPlayerCacheEntry get(UUID playerUUID) {
		return entries.get(playerUUID);
	}
	
	@Override
	public Iterable<UUID> getPlayers() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	/* Visible for GSPlayerCachePacket */
	void set(GSIPlayerCache playerCache) {
		entries.clear();
		for (UUID playerUUID : playerCache.getPlayers()) {
			// Note: entries are non-mutable
			entries.put(playerUUID, playerCache.get(playerUUID));
		}
		dispatchEntryAdded(null);
	}

	/* Visible for GSPlayerCacheEntryAddedPacket */
	void add(UUID playerUUID, GSPlayerCacheEntry entry) {
		if (!entries.containsKey(playerUUID)) {
			entries.put(playerUUID, entry);
			dispatchEntryAdded(playerUUID);
		}
	}
	
	/* Visible for GSPlayerCacheEntryRemovedPacket */
	void remove(UUID playerUUID) {
		if (entries.remove(playerUUID) != null)
			dispatchEntryRemoved(playerUUID);
	}
	
	public static GSPlayerCache read(GSDecodeBuffer buf) throws IOException {
		int count = buf.readInt();
		if (count < 0)
			throw new IOException("Player cache corrupted");
		Map<UUID, GSPlayerCacheEntry> entries = new HashMap<>();
		while (count-- != 0) {
			UUID playerUUID = buf.readUUID();
			GSPlayerCacheEntry entry = GSPlayerCacheEntry.read(buf);
			entries.put(playerUUID, entry);
		}
		return new GSPlayerCache(entries);
	}

	public static void write(GSEncodeBuffer buf, GSPlayerCache cache) throws IOException {
		buf.writeInt(cache.entries.size());
		for (Map.Entry<UUID, GSPlayerCacheEntry> entryKV : cache.entries.entrySet()) {
			buf.writeUUID(entryKV.getKey());
			GSPlayerCacheEntry.write(buf, entryKV.getValue());
		}
	}
}
