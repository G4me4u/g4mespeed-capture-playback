package com.g4mesoft.captureplayback.common.asset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.ui.util.GSPathUtil;
import com.g4mesoft.util.GSFileUtil;
import com.google.common.collect.Iterables;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetManager implements GSIAssetStorageListener 	{

	private static final String ASSET_DIRECTORY_NAME = "assets";
	
	private final GSIServerModuleManager manager;
	
	private final GSAssetStorage[] storages;
	private final GSIAssetHistory combinedHistory;
	private final GSIPlayerCache playerCache;

	private final List<GSIAssetStorageListener> listeners;
	
	public GSAssetManager(GSIServerModuleManager manager) {
		this.manager = manager;
		
		GSEAssetNamespace[] namespaces = GSEAssetNamespace.values();
		storages = new GSAssetStorage[namespaces.length];
		for (GSEAssetNamespace namespace : namespaces) {
			File dir = getAssetDirectory(namespace);
			GSAssetStorage storage = new GSAssetStorage(manager, namespace, dir);
			storages[namespace.getIndex()] = storage;
			storage.addListener(this);
		}
		combinedHistory = new GSCombinedAssetHistory(storages);
		playerCache = new GSCombinedPlayerCache(storages);
		
		listeners = new ArrayList<>();
	}
	
	public void init() {
		// Initialize storages (except for global)
		for (GSAssetStorage storage : storages) {
			if (storage.getNamespace() != GSEAssetNamespace.GLOBAL)
				storage.init();
		}
		// Backwards compatibility check with old storage format.
		GSAssetStorage globalStorage = getStorage(GSEAssetNamespace.GLOBAL);
		if (!globalStorage.init()) {
			// Storage does not exist in new format, attempt
			// to load the old storage format.
			try {
				loadOldStorageFormat();
			} catch (IOException e) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load old storage format", e);
			}
		}
	}
	
	private void loadOldStorageFormat() throws IOException {
		File dir = new File(manager.getCacheFile(), "compositions");
		try {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					if (!file.isFile()) {
						// Not an asset file
						continue;
					}
					// Check whether file is old format
					String fileExt = GSPathUtil.getFileExtension(file.getName());
					if (fileExt == null) {
						// No extension
						continue;
					}
					switch (fileExt) {
					case "gcomp":
						// File is old composition format
						loadOldCompositionFormat(file);
						break;
					case "gsq":
						// File is old sequence format
						loadOldSequenceFormat(file);
						break;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}
	
	private void loadOldCompositionFormat(File assetFile) throws IOException {
		if ("default_composition.gcomp".equals(assetFile.getName())) {
			// This is a duplicate composition
			return;
		}
		GSComposition composition;
		try {
			composition = GSFileUtil.readFile(assetFile, GSComposition::read);
		} catch (IOException e) {
			// Note: We are very conservative
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load old composition {}", assetFile.getName());
			return;
		}
		String name = composition.getName();
		if (name.isEmpty())
			name = getOldAssetFileName(assetFile);
		UUID assetUUID = createAsset(
			GSEAssetType.COMPOSITION,
			GSEAssetNamespace.GLOBAL,
			name,
			GSAssetInfo.UNKNOWN_OWNER_UUID
		);
		// Update the asset itself
		GSAssetRef ref = requestAsset(assetUUID);
		if (ref != null && ref.get() instanceof GSCompositionAsset) {
			GSCompositionAsset asset = (GSCompositionAsset)ref.get();
			asset.getComposition().duplicateFrom(composition);
		}
		ref.release();
	}
	
	private void loadOldSequenceFormat(File assetFile) {
		if ("active_sequence.gsq".equals(assetFile.getName())) {
			// This is a duplicate sequence
			return;
		}
		GSSequence sequence;
		try {
			sequence = GSFileUtil.readFile(assetFile, GSSequence::read);
		} catch (IOException e) {
			// Note: We are very conservative, especially with
			// the old sequence formats (since it changed a lot).
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load old sequence {}", assetFile.getName());
			return;
		}
		String name = sequence.getName();
		if (name.isEmpty())
			name = getOldAssetFileName(assetFile);
		UUID assetUUID = createAsset(
			GSEAssetType.SEQUENCE,
			GSEAssetNamespace.GLOBAL,
			name,
			GSAssetInfo.UNKNOWN_OWNER_UUID
		);
		// Update the asset itself
		GSAssetRef ref = requestAsset(assetUUID);
		if (ref != null && ref.get() instanceof GSSequenceAsset) {
			GSSequenceAsset asset = (GSSequenceAsset)ref.get();
			asset.getSequence().duplicateFrom(sequence);
		}
		ref.release();
	}
	
	private String getOldAssetFileName(File assetFile) {
		String name = assetFile.getName();
		int dotIdx = name.lastIndexOf('.');
		if (dotIdx != -1)
			return name.substring(0, dotIdx);
		return name;
	}

	private File getAssetDirectory(GSEAssetNamespace namespace) {
		switch (namespace) {
		case GLOBAL:
			return getAssetDirectory(manager.getCacheFile());
		case WORLD:
			return getAssetDirectory(manager.getWorldCacheFile());
		}
		throw new IllegalStateException("Unknown namespace: " + namespace);
	}

	private File getAssetDirectory(File cacheDirectory) {
		return new File(cacheDirectory, ASSET_DIRECTORY_NAME);
	}
	
	public void addListener(GSIAssetStorageListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	public void removeListener(GSIAssetStorageListener listener) {
		listeners.remove(listener);
	}
	
	public boolean hasAssetHandle(GSAssetHandle handle) {
		return getStorage(handle.getNamespace()).hasAssetHandle(handle);
	}

	public boolean hasAsset(UUID assetUUID) {
		return getStorage(assetUUID) != null;
	}

	public GSAssetInfo getInfo(UUID assetUUID) {
		GSAssetStorage storage = getStorage(assetUUID);
		return (storage != null) ? storage.getInfo(assetUUID) : null;
	}
	
	public GSAssetInfo getInfoFromHandle(GSAssetHandle handle) {
		return getStorage(handle.getNamespace()).getInfoFromHandle(handle);
	}
	
	public boolean isLoaded(UUID assetUUID) {
		GSAssetStorage storage = getStorage(assetUUID);
		return storage != null && storage.isLoaded(assetUUID);
	}
	
	public boolean hasPermission(ServerPlayerEntity player, UUID assetUUID) {
		GSAssetInfo info = getInfo(assetUUID);
		return info != null && info.hasPermission(player);
	}

	public boolean hasPermission(ServerPlayerEntity player, GSAssetHandle handle) {
		GSAssetInfo info = getInfoFromHandle(handle);
		return info != null && info.hasPermission(player);
	}

	public UUID createAsset(GSEAssetType type, GSEAssetNamespace namespace, String name, UUID ownerUUID) {
		GSAssetHandle handle = GSAssetHandle.fromNameUnique(namespace, name, this::hasAssetHandle);
		return createAsset(type, handle, name, ownerUUID);
	}
	
	public UUID createAsset(GSEAssetType type, GSAssetHandle handle, String name, UUID createdByUUID) {
		if (hasAssetHandle(handle)) {
			// Fallback for unlikely cases where handle already exists.
			handle = GSAssetHandle.fromNameUnique(handle.getNamespace(), name, this::hasAssetHandle);
		}
		UUID assetUUID = GSUUIDUtil.randomUnique(this::hasAsset);
		GSAssetStorage storage = getStorage(handle.getNamespace());
		storage.createAsset(new GSAssetInfo(
			type, 
			assetUUID,
			handle,
			name,
			System.currentTimeMillis(),
			createdByUUID,
			createdByUUID
		));
		return assetUUID;
	}
	
	public UUID createDuplicateAsset(GSEAssetNamespace namespace, String name, UUID ownerUUID, UUID originalAssetUUID) {
		GSAssetHandle handle = GSAssetHandle.fromNameUnique(namespace, name, this::hasAssetHandle);
		return createDuplicateAsset(handle, name, ownerUUID, originalAssetUUID);
	}
	
	public UUID createDuplicateAsset(GSAssetHandle handle, String name, UUID ownerUUID, UUID originalAssetUUID) {
		if (hasAssetHandle(handle)) {
			// Fallback for unlikely cases where handle already exists.
			handle = GSAssetHandle.fromNameUnique(handle.getNamespace(), name, this::hasAssetHandle);
		}
		// Note: synchronous read
		GSAssetRef ref = requestAsset(originalAssetUUID);
		GSAssetInfo info = getInfo(originalAssetUUID);
		if (ref == null || info == null)
			throw new IllegalArgumentException("Original asset does not exist");
		final GSAssetHandle fHandle = handle;
		UUID assetUUID = GSUUIDUtil.randomUnique(this::hasAsset);
		GSAssetStorage storage = getStorage(fHandle.getNamespace());
		storage.createDuplicateAsset(new GSAssetInfo(
			ref.get().getType(),
			assetUUID,
			fHandle,
			name,
			System.currentTimeMillis(),
			info.getCreatedByUUID(),
			ownerUUID
		), ref.get());
		ref.release();
		return assetUUID;
	}
	
	public void deleteAsset(UUID assetUUID) {
		GSAssetStorage storage = getStorage(assetUUID);
		if (storage != null)
			storage.deleteAsset(assetUUID);
	}
	
	public GSAssetRef requestAsset(UUID assetUUID) {
		GSAssetStorage storage = getStorage(assetUUID);
		return (storage != null) ? storage.requestAsset(assetUUID) : null;
	}
	
	public void unloadAll() {
		for (GSAssetStorage storage : storages)
			storage.unloadAll();
	}
	
	public GSIAssetHistory getStoredHistory() {
		return combinedHistory;
	}
	
	public GSIPlayerCache getPlayerCache() {
		return playerCache;
	}
	
	private GSAssetStorage getStorage(GSEAssetNamespace namespace) {
		return storages[namespace.getIndex()];
	}

	private GSAssetStorage getStorage(UUID assetUUID) {
		for (GSAssetStorage storage : storages) {
			if (storage.hasAsset(assetUUID))
				return storage;
		}
		return null;
	}

	@Override
	public void onAssetAdded(UUID assetUUID) {
		for (GSIAssetStorageListener listener : listeners)
			listener.onAssetAdded(assetUUID);
	}

	@Override
	public void onAssetRemoved(UUID assetUUID) {
		for (GSIAssetStorageListener listener : listeners)
			listener.onAssetRemoved(assetUUID);
	}
	
	private static class GSCombinedAssetHistory implements GSIAssetHistory,
	                                                       GSIAssetHistoryListener {

		private final GSIAssetHistory[] histories;
		private final List<GSIAssetHistoryListener> listeners;
		
		public GSCombinedAssetHistory(GSAssetStorage[] storages) {
			histories = new GSIAssetHistory[storages.length];
			for (int i = 0; i < storages.length; i++) {
				GSIAssetHistory history = storages[i].getStoredHistory();
				histories[i] = history;
				history.addListener(this);
			}
			listeners = new ArrayList<>();
		}
		
		@Override
		public void addListener(GSIAssetHistoryListener listener) {
			if (listener == null)
				throw new IllegalArgumentException("listener is null!");
			listeners.add(listener);
		}

		@Override
		public void removeListener(GSIAssetHistoryListener listener) {
			listeners.remove(listener);
		}

		@Override
		public boolean contains(UUID assetUUID) {
			for (GSIAssetHistory history : histories) {
				if (history.contains(assetUUID))
					return true;
			}
			return false;
		}

		@Override
		public boolean containsHandle(GSAssetHandle handle) {
			return histories[handle.getNamespace().getIndex()].containsHandle(handle);
		}

		@Override
		public GSAssetInfo get(UUID assetUUID) {
			for (GSIAssetHistory history : histories) {
				GSAssetInfo info = history.get(assetUUID);
				if (info != null)
					return info;
			}
			return null;
		}

		@Override
		public GSAssetInfo getFromHandle(GSAssetHandle handle) {
			return histories[handle.getNamespace().getIndex()].getFromHandle(handle);
		}

		@Override
		public void add(GSAssetInfo info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addAll(Iterable<GSAssetInfo> iterable) {
			throw new UnsupportedOperationException();
		}

		@Override
		public GSAssetInfo remove(UUID assetUUID) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(GSIAssetHistory other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			int s = 0;
			for (GSIAssetHistory history : histories)
				s += history.size();
			return s;
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}
		
		@Override
		public Collection<GSAssetInfo> asCollection() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Iterator<GSAssetInfo> iterator() {
			return Iterables.concat(histories).iterator();
		}

		@Override
		public void onHistoryChanged(UUID assetUUID) {
			for (GSIAssetHistoryListener listener : listeners)
				listener.onHistoryChanged(assetUUID);
		}
	}
	
	private static class GSCombinedPlayerCache extends GSAbstractPlayerCache
	                                           implements GSIPlayerCacheListener {

		private final GSIPlayerCache[] caches;
		
		public GSCombinedPlayerCache(GSAssetStorage[] storages) {
			caches = new GSIPlayerCache[storages.length];
			for (int i = 0; i < storages.length; i++) {
				GSIPlayerCache cache = storages[i].getPlayerCache();
				caches[i] = cache;
				cache.addListener(this);
			}
		}
		
		@Override
		public GSPlayerCacheEntry get(UUID playerUUID) {
			for (GSIPlayerCache cache : caches) {
				GSPlayerCacheEntry entry = cache.get(playerUUID);
				if (entry != null)
					return entry;
			}
			return null;
		}

		@Override
		public Iterable<UUID> getPlayers() {
			return () -> new GSCombinedPlayerCacheIterator(caches);
		}

		@Override
		public void onEntryAdded(UUID playerUUID) {
			if (playerUUID == null || getEntryCount(playerUUID) == 1) {
				// The entry was added by this invocation
				dispatchEntryAdded(playerUUID);
			}
		}

		@Override
		public void onEntryRemoved(UUID playerUUID) {
			if (getEntryCount(playerUUID) == 0) {
				// The entry is removed by all
				dispatchEntryRemoved(playerUUID);
			}
		}
		
		private int getEntryCount(UUID playerUUID) {
			int cnt = 0;
			for (GSIPlayerCache cache : caches) {
				if (cache.get(playerUUID) != null)
					cnt++;
			}
			return cnt;
		}
	}
	
	private static class GSCombinedPlayerCacheIterator implements Iterator<UUID> {

		private final GSIPlayerCache[] caches;
		private int cur;

		private Iterator<UUID> itr;
		private UUID nextElem;
		
		public GSCombinedPlayerCacheIterator(GSIPlayerCache[] caches) {
			this.caches = caches;
			cur = 0;
			// Prepare first iterator
			prepareCurIter();
		}
		
		private void prepareCurIter() {
			if (cur < caches.length) {
				itr = caches[cur].getPlayers().iterator();
			} else {
				// Let GC do its job
				itr = null;
			}
		}

		private void prepareNext() {
			while (itr != null && !itr.hasNext()) {
				cur++;
				prepareCurIter();
			}
			// Note: either itr == null or itr.hasNext().
			nextElem = (itr != null) ? itr.next() : null;
		}
		
		@Override
		public boolean hasNext() {
			if (nextElem != null) {
				// Already prepared element
				return true;
			}
			// Prepare element that has not been seen before.
			do {
				prepareNext();
				if (nextElem == null)
					return false;
			} while (isElementSeen(nextElem));
			//assert(nextElem != null && !isElementSeen(nextElem))
			return true;
		}

		private boolean isElementSeen(UUID elem) {
			for (int i = 0; i < cur; i++) {
				if (caches[i].get(elem) != null)
					return true;
			}
			return false;
		}
		
		@Override
		public UUID next() {
			if (!hasNext())
				throw new NoSuchElementException();
			UUID elem = nextElem;
			nextElem = null;
			return elem;
		}
	}
}
