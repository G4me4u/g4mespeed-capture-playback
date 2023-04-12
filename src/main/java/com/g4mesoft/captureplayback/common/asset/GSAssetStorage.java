package com.g4mesoft.captureplayback.common.asset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;
import com.g4mesoft.util.GSFileUtil;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetStorage {

	private static final String ASSET_FILE_NAME_TEMPLATE = "%s.gsa";
	private static final String HISTORY_FILE_NAME = "history.bin";
	private static final String PLAYER_CACHE_FILE_NAME = "players.bin";
	
	private final GSIServerModuleManager manager;
	private final GSEAssetNamespace namespace;
	private final File assetDir;
	
	private final GSAssetHistory storedHistory;
	private final GSAssetHistory derivedHistory;

	private final GSPersistentPlayerCache playerCache;
	
	private final Map<UUID, GSAbstractAsset> loadedAssets;
	private final Map<UUID, GSAssetRef> activeRefs;
	
	private final Map<UUID, GSAssetListener> assetListeners;
	private final List<GSIAssetStorageListener> listeners;
	
	public GSAssetStorage(GSIServerModuleManager manager, GSEAssetNamespace namespace, File assetDir) {
		this.manager = manager;
		this.namespace = namespace;
		this.assetDir = assetDir;
		
		storedHistory = new GSAssetHistory();
		derivedHistory = new GSAssetHistory();

		playerCache = new GSPersistentPlayerCache();
		
		loadedAssets = new HashMap<>();
		activeRefs = new HashMap<>();
		
		assetListeners = new HashMap<>();
		listeners = new ArrayList<>();
	}

	public boolean init() {
		if (loadStoredHistory()) {
			loadPlayerCache();
			return true;
		}
		return false;
	}
	
	public void addListener(GSIAssetStorageListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	public void removeListener(GSIAssetStorageListener listener) {
		listeners.remove(listener);
	}
	
	private void dispatchAssetAdded(UUID assetUUID) {
		for (GSIAssetStorageListener listener : listeners)
			listener.onAssetAdded(assetUUID);
	}

	private void dispatchAssetRemoved(UUID assetUUID) {
		for (GSIAssetStorageListener listener : listeners)
			listener.onAssetRemoved(assetUUID);
	}
	
	public boolean hasStoredAsset(UUID assetUUID) {
		return storedHistory.contains(assetUUID);
	}

	public boolean hasAssetHandle(GSAssetHandle handle) {
		return storedHistory.containsHandle(handle);
	}

	public boolean hasAsset(UUID assetUUID) {
		return hasStoredAsset(assetUUID) || derivedHistory.contains(assetUUID);
	}
	
	public GSAssetInfo getInfo(UUID assetUUID) {
		GSAssetInfo storedInfo = storedHistory.get(assetUUID);
		return (storedInfo != null) ? storedInfo : derivedHistory.get(assetUUID);
	}
	
	public GSAssetInfo getInfoFromHandle(GSAssetHandle handle) {
		return storedHistory.getFromHandle(handle);
	}
	
	public GSAssetRef requestAsset(UUID assetUUID) {
		GSAssetRef ref = activeRefs.get(assetUUID);
		if (ref != null)
			return ref.retain();
		// Attempt to load from storage
		if (ensureLoaded(assetUUID)) {
			GSAbstractAsset asset = loadedAssets.get(assetUUID);
			//assert(asset != null)
			ref = new GSAssetRef(this, asset);
			//assert(ref.refCnt() == 1)
			activeRefs.put(assetUUID, ref);
			return ref;
		}
		// Unable to load asset
		return null;
	}
	
	private boolean ensureLoaded(UUID assetUUID) {
		if (isLoaded(assetUUID))
			return true;
		GSAssetInfo info = storedHistory.get(assetUUID);
		if (info != null) {
			GSDecodedAssetFile assetFile;
			try {
				assetFile = GSFileUtil.readFile(getAssetFile(info), GSDecodedAssetFile::read);
				addAsset(info, assetFile.getAsset());
				return true;
			} catch (Throwable t) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load asset ({})", info.getAssetUUID(), t);
				return false;
			}
		}
		return false;
	}
	
	public boolean isLoaded(UUID assetUUID) {
		return loadedAssets.containsKey(assetUUID);
	}
	
	private void checkDistinctAssetUUID(UUID assetUUID) {
		if (hasAsset(assetUUID))
			throw new IllegalArgumentException("Asset with UUID: " + assetUUID + " already exists");
	}
	
	private void checkCorrespondingInfo(GSAssetInfo info, GSAbstractAsset asset) {
		if (!info.getType().equals(asset.getType()))
			throw new IllegalArgumentException("Asset type does not match asset info");
		if (!info.getAssetUUID().equals(asset.getUUID()))
			throw new IllegalArgumentException("Asset UUID does not match asset info");
	}
	
	private void checkDistinctHandle(GSAssetHandle handle) {
		if (handle == null || hasAssetHandle(handle))
			throw new IllegalArgumentException("Stored assets must specify a unique handle!");
	}
	
	private void checkHandleNamespace(GSAssetHandle handle) {
		if (handle.getNamespace() != namespace)
			throw new IllegalArgumentException("Asset namespace does not match");
	}

	public void createAsset(GSAssetInfo info) {
		createAsset(info, GSAssetRegistry.getConstr(info.getType()).apply(info));
	}
	
	public void createDuplicateAsset(GSAssetInfo info, GSAbstractAsset originalAsset) {
		GSAbstractAsset asset = GSAssetRegistry.getConstr(originalAsset.getType()).apply(info);
		asset.duplicateFrom(originalAsset);
		// Note: The check for asset type in info and corresponding
		//       original asset is checked below.
		createAsset(info, asset);
	}
	
	private void createAsset(GSAssetInfo info, GSAbstractAsset asset) {
		checkDistinctAssetUUID(info.getAssetUUID());
		checkCorrespondingInfo(info, asset);
		checkDistinctHandle(info.getHandle());
		checkHandleNamespace(info.getHandle());
		// Note: add to player cache first so clients know about names
		//       at the time the history updates.
		playerCache.onAssetAdded(this, info);
		storedHistory.add(info);
		addAsset(info, asset);
		// Note: immediately unload asset so history is consistent.
		unloadAsset(info.getAssetUUID());
	}

	/* 
	 * Adds the asset to loadedAssets. The assetUUID must not already be loaded,
	 * and the asset info *must* correspond to the given asset.
	 */
	private void addAsset(GSAssetInfo info, GSAbstractAsset asset) {
		UUID assetUUID = info.getAssetUUID();
		loadedAssets.put(assetUUID, asset);
		// Add the derived assets as well
		Iterator<UUID> itr = asset.getDerivedIterator();
		while (itr.hasNext())
			addDerivedAsset(assetUUID, itr.next());
		// Listen for new derived assets
		GSAssetListener listener = new GSAssetListener(assetUUID);
		asset.addListener(listener);
		assetListeners.put(assetUUID, listener);
		asset.onAdded();
		dispatchAssetAdded(assetUUID);
	}
	
	private void addDerivedAsset(UUID parentUUID, UUID derivedUUID) {
		checkDistinctAssetUUID(derivedUUID);
		GSAssetInfo info = getInfo(parentUUID);
		GSAbstractAsset asset = loadedAssets.get(parentUUID);
		if (info != null && asset != null) {
			GSAbstractAsset derivedAsset = asset.getDerivedAsset(derivedUUID);
			GSAssetInfo derivedInfo = new GSAssetInfo(derivedAsset.getType(), derivedUUID, info);
			if (derivedInfo.getHandle() != null)
				throw new IllegalStateException("Derived assets are not allowed to have a handle!");
			// Ensure that we do not get into an invalid state
			checkCorrespondingInfo(derivedInfo, derivedAsset);
			// Recursively add asset and its derived assets
			derivedHistory.add(derivedInfo);
			addAsset(derivedInfo, derivedAsset);
		}
	}
	
	private boolean loadStoredHistory() {
		GSAssetHistory history = null;
		try {
			history = GSFileUtil.readFile(getHistoryFile(), GSAssetHistory::read);
		} catch (IOException e) {
		}

		if (history == null || !isValidHistory(history)) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load history ({})", namespace.getName());
			return false;
		}
		// Save and remove assets that no longer exist.
		for (GSAssetInfo info : storedHistory) {
			if (!history.contains(info.getAssetUUID())) {
				// Note: Similar to #deleteAsset(...), but without
				// modifying storedHistory.
				saveAsset(info.getAssetUUID());
				removeAsset(info.getAssetUUID());
				playerCache.onAssetRemoved(this, info);
			}
		}
		storedHistory.set(history);
		return true;
	}

	private boolean isValidHistory(GSAssetHistory history) {
		// Ensure that the asset namespace matches
		for (GSAssetInfo info : history) {
			GSAssetHandle handle = info.getHandle();
			if (handle == null || handle.getNamespace() != namespace)
				return false;
		}
		return true;
	}
	
	private boolean loadPlayerCache() {
		GSPersistentPlayerCache cache = null;
		try {
			cache = GSFileUtil.readFile(getPlayerCacheFile(), GSPersistentPlayerCache::read);
		} catch (IOException e) {
		}
		
		if (cache == null) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load player cache ({})", namespace.getName());
			return false;
		}
		playerCache.set(cache);
		return true;
	}
	
	public void deleteAsset(UUID assetUUID) {
		GSAssetInfo info = storedHistory.get(assetUUID);
		if (info != null) {
			// Unload and save latest asset
			unloadAsset(assetUUID);
			playerCache.onAssetRemoved(this, info);
			// Delete from history, but keep asset file for
			// recovery. The asset UUID might be reused later
			storedHistory.remove(assetUUID);
			saveHistory(assetUUID);
		}
	}
	
	public void unloadAsset(UUID assetUUID) {
		if (hasStoredAsset(assetUUID) && loadedAssets.containsKey(assetUUID)) {
			save(assetUUID);
			removeAsset(assetUUID);
		}
	}
	
	public void unloadAll() {
		if (!loadedAssets.isEmpty()) {
			UUID[] assetUUIDs = loadedAssets.keySet().toArray(new UUID[0]);
			for (UUID assetUUID : assetUUIDs) {
				saveAsset(assetUUID);
				removeAsset(assetUUID);
			}
			saveHistory(null);
		}
	}
	
	private void removeAsset(UUID assetUUID) {
		GSAssetInfo info = getInfo(assetUUID);
		GSAbstractAsset asset = loadedAssets.get(assetUUID);
		if (asset != null && info != null) {
			GSAssetListener listener = assetListeners.remove(assetUUID);
			if (listener != null)
				listener.setRemoved(true);
			// Remove derived assets first
			Iterator<UUID> itr = asset.getDerivedIterator();
			while (itr.hasNext())
				removeAsset(itr.next());
			// Remove derived listener from asset
			asset.removeListener(listener);
			// Remove asset itself
			loadedAssets.remove(assetUUID);
			// Invalidate references
			GSAssetRef ref = activeRefs.remove(assetUUID);
			if (ref != null)
				ref.invalidate();
			if (info.isDerived())
				derivedHistory.remove(assetUUID);
			asset.onRemoved();
			dispatchAssetRemoved(assetUUID);
		}
	}

	public boolean save(UUID assetUUID) {
		if (saveAsset(assetUUID))
			return saveHistory(assetUUID);
		return false;
	}
	
	public boolean saveAll() {
		boolean success = true;
		for (UUID assetUUID : loadedAssets.keySet()) {
			if (!saveAsset(assetUUID))
				success = false;
		}
		// Save the updated history
		if (!saveHistory(null))
			success = false;
		return success;
	}
	
	public boolean saveAsset(UUID assetUUID) {
		final GSAssetInfo info = storedHistory.get(assetUUID);
		if (info != null && !info.isDerived()) {
			GSAbstractAsset asset = loadedAssets.get(assetUUID);
			if (asset != null) {
				long saveTimeMs = System.currentTimeMillis();
				// Save the asset itself
				GSAssetFileHeader header = new GSAssetFileHeader(info);
				GSDecodedAssetFile assetFile = new GSDecodedAssetFile(header, asset);
				try {
					GSFileUtil.writeFile(getAssetFile(info), assetFile, GSDecodedAssetFile::write);
				} catch (IOException ignore) {
					return false;
				}
				// Update last modified time
				storedHistory.setLastModifiedTimestamp(assetUUID, saveTimeMs);
				return true;
			}
		}
		return false;
	}
	
	public boolean saveHistory(UUID assetUUID) {
		try {
			GSFileUtil.writeFile(getHistoryFile(), storedHistory, GSAssetHistory::write);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to save history ({})", namespace.getName());
			return false;
		}
		// Player cache should always be consistent
		// with the asset history.
		return savePlayerCache();
	}
	
	private boolean savePlayerCache() {
		try {
			GSFileUtil.writeFile(getPlayerCacheFile(), playerCache, GSPersistentPlayerCache::write);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to save player cache ({})", namespace.getName());
			return false;
		}
		return true;
	}
	
	public GSEAssetNamespace getNamespace() {
		return namespace;
	}
	
	public File getAssetDir() {
		return assetDir;
	}
	
	private File getHistoryFile() {
		return new File(assetDir, HISTORY_FILE_NAME);
	}
	
	private File getAssetFile(GSAssetInfo info) {
		return new File(assetDir, String.format(ASSET_FILE_NAME_TEMPLATE, info.getAssetUUID()));
	}
	
	private File getPlayerCacheFile() {
		return new File(assetDir, PLAYER_CACHE_FILE_NAME);
	}

	public GSIAssetHistory getStoredHistory() {
		return new GSUnmodifiableAssetHistory(storedHistory);
	}
	
	public GSIPlayerCache getPlayerCache() {
		return playerCache;
	}

	private class GSAssetListener implements GSIAssetListener {
		
		private final UUID assetUUID;
		private boolean removed;
		
		public GSAssetListener(UUID assetUUID) {
			this.assetUUID = assetUUID;
			removed = false;
		}
		
		public void setRemoved(boolean removed) {
			this.removed = removed;
		}
		
		@Override
		public void onNameChanged(String name) {
			storedHistory.setAssetName(assetUUID, name);
		}

		@Override
		public void onDerivedAssetRemoved(UUID derivedUUID) {
			removeAsset(derivedUUID);
		}
		
		@Override
		public void onDerivedAssetAdded(UUID derivedUUID) {
			if (!removed) {
				// Do not add new assets when parent is removed.
				addDerivedAsset(assetUUID, derivedUUID);
			}
		}
	}
	
	private static class GSPersistentPlayerCache extends GSAbstractPlayerCache {

		private final Map<UUID, GSPlayerCacheEntryRef> entries;

		public GSPersistentPlayerCache() {
			entries = new HashMap<>();
		}

		@Override
		public GSPlayerCacheEntry get(UUID playerUUID) {
			GSPlayerCacheEntryRef ref = entries.get(playerUUID);
			return (ref != null) ? ref.entry : null;
		}
		
		@Override
		public Iterable<UUID> getPlayers() {
			return Collections.unmodifiableSet(entries.keySet());
		}
		
		/* Visible for GSAssetStorage */
		void onAssetAdded(GSAssetStorage storage, GSAssetInfo info) {
			incRef(storage, info, 1);
		}

		/* Visible for GSAssetStorage */
		void onAssetRemoved(GSAssetStorage storage, GSAssetInfo info) {
			incRef(storage, info, -1);
		}

		private void incRef(GSAssetStorage storage, GSAssetInfo info, int sign) {
			incRef(storage, info.getOwnerUUID(), sign);
			for (UUID playerUUID : info.getPermissionUUIDs())
				incRef(storage, playerUUID, sign);
		}
		
		private void incRef(GSAssetStorage storage, UUID playerUUID, int sign) {
			GSPlayerCacheEntryRef ref = entries.get(playerUUID);
			if (ref == null) {
				if (sign > 0) {
					// Note: the extra check is required in case the
					// cache is inconsistent with the asset history.
					ref = createEntry(storage, playerUUID);
				}
				if (ref == null) {
					// Player not found or sign < 0
					return;
				}
				entries.put(playerUUID, ref);
				dispatchEntryAdded(playerUUID);
			}
			ref.refCount += sign;
			if (ref.refCount <= 0) {
				entries.remove(playerUUID);
				dispatchEntryRemoved(playerUUID);
			}
		}
		
		private GSPlayerCacheEntryRef createEntry(GSAssetStorage storage, UUID playerUUID) {
			ServerPlayerEntity player = storage.manager.getPlayer(playerUUID);
			if (player != null) {
				String name = player.getEntityName();
				GSPlayerCacheEntry entry = new GSPlayerCacheEntry(name);
				// Note: refCount immediately incremented in #incRef(...)
				return new GSPlayerCacheEntryRef(0, entry);
			}
			return null;
		}
		
		private void set(GSPersistentPlayerCache other) {
			entries.clear();
			entries.putAll(other.entries);
			dispatchEntryAdded(null);
		}
		
		public static GSPersistentPlayerCache read(GSDecodeBuffer buf) throws IOException {
			int count = buf.readInt();
			if (count < 0)
				throw new IOException("Player cache corrupted");
			GSPersistentPlayerCache cache = new GSPersistentPlayerCache();
			while (count-- != 0) {
				UUID playerUUID = buf.readUUID();
				GSPlayerCacheEntryRef ref = GSPlayerCacheEntryRef.read(buf);
				cache.entries.put(playerUUID, ref);
			}
			return cache;
		}

		public static void write(GSEncodeBuffer buf, GSPersistentPlayerCache cache) throws IOException {
			buf.writeInt(cache.entries.size());
			for (Map.Entry<UUID, GSPlayerCacheEntryRef> entryKV : cache.entries.entrySet()) {
				buf.writeUUID(entryKV.getKey());
				GSPlayerCacheEntryRef.write(buf, entryKV.getValue());
			}
		}
	}
	
	private static class GSPlayerCacheEntryRef {

		private int refCount;
		private final GSPlayerCacheEntry entry;
		
		public GSPlayerCacheEntryRef(int refCount, GSPlayerCacheEntry entry) {
			if (entry == null)
				throw new IllegalArgumentException("entry is null");
			if (refCount < 0)
				throw new IllegalArgumentException("refCount must be non-negative");
			this.entry = entry;
			this.refCount = refCount;
		}
		
		public static GSPlayerCacheEntryRef read(GSDecodeBuffer buf) throws IOException {
			int refCount = buf.readInt();
			if (refCount <= 0)
				throw new IOException("Player entry corrupted");
			GSPlayerCacheEntry entry = GSPlayerCacheEntry.read(buf);
			return new GSPlayerCacheEntryRef(refCount, entry);
		}
		
		public static void write(GSEncodeBuffer buf, GSPlayerCacheEntryRef ref) throws IOException {
			buf.writeInt(ref.refCount);
			GSPlayerCacheEntry.write(buf, ref.entry);
		}
	}
}
