package com.g4mesoft.captureplayback.common.asset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.util.GSFileUtil;
import com.g4mesoft.util.GSFileUtil.GSFileEncoder;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSAssetStorage {

	private static final String ASSET_FILE_NAME_TEMPLATE = "%s.gsa";
	private static final String HISTORY_FILE_NAME = "history.bin";
	
	private final File assetDir;
	
	private final GSAssetHistory storedHistory;
	private final GSAssetHistory derivedHistory;
	private final Map<UUID, GSAbstractAsset> activeAssets;
	
	private final Map<UUID, GSAssetListener> assetListeners;
	private final List<GSIAssetStorageListener> listeners;
	
	public GSAssetStorage(File assetDir) {
		this.assetDir = assetDir;
		
		storedHistory = new GSAssetHistory();
		derivedHistory = new GSAssetHistory();
		activeAssets = new HashMap<>();
		
		assetListeners = new HashMap<>();
		listeners = new ArrayList<>();
	}

	public void addListener(GSIAssetStorageListener listener) {
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

	public boolean hasAsset(UUID assetUUID) {
		return hasStoredAsset(assetUUID) || derivedHistory.contains(assetUUID);
	}
	
	public GSAssetInfo getInfo(UUID assetUUID) {
		GSAssetInfo storedInfo = storedHistory.get(assetUUID);
		return (storedInfo != null) ? storedInfo : derivedHistory.get(assetUUID);
	}
	
	public GSAbstractAsset requestAsset(UUID assetUUID) {
		return ensureLoaded(assetUUID) ? activeAssets.get(assetUUID) : null;
	}
	
	public boolean requestReadOnce(Consumer<GSAbstractAsset> action, UUID assetUUID) {
		boolean wasLoaded = isLoaded(assetUUID);
		GSAbstractAsset asset = requestAsset(assetUUID);
		if (asset != null) {
			action.accept(asset);
			if (!wasLoaded) {
				// Do not store asset or history.
				removeAsset(assetUUID);
			}
			return true;
		}
		return false;
	}

	private boolean ensureLoaded(UUID assetUUID) {
		if (isLoaded(assetUUID))
			return true;
		GSAssetInfo info = storedHistory.get(assetUUID);
		if (info != null) {
			GSAbstractAsset asset;
			try {
				asset = GSFileUtil.readFile(getAssetFile(info), GSAssetRegistry.getDecoder(info.getType()));
				checkCorrespondingInfo(info, asset);
			} catch (Throwable ignore) {
				return false;
			}
			addAsset(info, asset);
			return true;
		}
		return false;
	}
	
	public boolean isLoaded(UUID assetUUID) {
		return activeAssets.containsKey(assetUUID);
	}
	
	public boolean hasPermission(ServerPlayerEntity player, UUID assetUUID) {
		GSAssetInfo info = getInfo(assetUUID);
		return info != null && info.hasPermission(player);
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

	public void createAsset(GSAssetInfo info) {
		createAsset(info, GSAssetRegistry.getConstr(info.getType()).apply(info));
	}
	
	public void createAsset(GSAssetInfo info, GSAbstractAsset asset) {
		checkDistinctAssetUUID(info.getAssetUUID());
		checkCorrespondingInfo(info, asset);
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
		activeAssets.put(assetUUID, asset);
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
		GSAbstractAsset asset = requestAsset(parentUUID);
		if (info != null && asset != null) {
			GSAbstractAsset derivedAsset = asset.getDerivedAsset(derivedUUID);
			GSAssetInfo derivedInfo = new GSAssetInfo(derivedAsset.getType(), derivedUUID, info);
			// Ensure that we do not get into an invalid state
			checkCorrespondingInfo(derivedInfo, derivedAsset);
			// Recursively add asset and its derived assets
			derivedHistory.add(derivedInfo);
			addAsset(derivedInfo, derivedAsset);
		}
	}
	
	public void loadStoredHistory() {
		GSAssetHistory history = null;
		try {
			history = GSFileUtil.readFile(getHistoryFile(), GSAssetHistory::read);
		} catch (IOException e) {
		}

		if (history == null) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to load history");
		} else {
			// Save and remove assets that no longer exist.
			for (GSAssetInfo info : storedHistory) {
				if (!history.contains(info.getAssetUUID())) {
					saveAsset(info.getAssetUUID());
					removeAsset(info.getAssetUUID());
				}
			}
			storedHistory.set(history);
		}
	}

	public void unloadAsset(UUID assetUUID) {
		if (hasStoredAsset(assetUUID) && activeAssets.containsKey(assetUUID)) {
			save(assetUUID);
			removeAsset(assetUUID);
		}
	}
	
	public void unloadAll() {
		if (!activeAssets.isEmpty()) {
			UUID[] assetUUIDs = activeAssets.keySet().toArray(new UUID[0]);
			for (UUID assetUUID : assetUUIDs) {
				saveAsset(assetUUID);
				removeAsset(assetUUID);
			}
			saveHistory(null);
		}
	}
	
	private void removeAsset(UUID assetUUID) {
		GSAssetInfo info = getInfo(assetUUID);
		GSAbstractAsset asset = activeAssets.get(assetUUID);
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
			activeAssets.remove(assetUUID);
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
		for (UUID assetUUID : activeAssets.keySet()) {
			if (!saveAsset(assetUUID))
				success = false;
		}
		// Save the updated history
		if (!saveHistory(null))
			success = false;
		return success;
	}
	
	public boolean saveAsset(UUID assetUUID) {
		GSAssetInfo info = storedHistory.get(assetUUID);
		if (info != null && !info.isDerived()) {
			GSAbstractAsset asset = activeAssets.get(assetUUID);
			if (asset != null) {
				long saveTimeMs = System.currentTimeMillis();
				// Save the asset itself
				try {
					writeAsset(getAssetFile(info), asset, GSAssetRegistry.getEncoder(info.getType()));
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

	@SuppressWarnings("unchecked")
	private <T extends GSAbstractAsset> void writeAsset(File file, GSAbstractAsset asset, GSFileEncoder<T> encoder) throws IOException {
		GSFileUtil.writeFile(file, (T)asset, encoder);
	}

	public boolean saveHistory(UUID assetUUID) {
		try {
			GSFileUtil.writeFile(getHistoryFile(), storedHistory, GSAssetHistory::write);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to save history");
			return false;
		}
		return true;
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

	public GSIAssetHistory getStoredHistory() {
		return new GSUnmodifiableAssetHistory(storedHistory);
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
}
