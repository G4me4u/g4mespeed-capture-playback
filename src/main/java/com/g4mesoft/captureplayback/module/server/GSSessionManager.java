package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAbstractAsset;
import com.g4mesoft.captureplayback.common.asset.GSAssetStorage;
import com.g4mesoft.captureplayback.common.asset.GSIAssetStorageListener;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.server.GSIServerModuleManager;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionManager implements GSIAssetStorageListener {

	private final GSIServerModuleManager manager;
	private final GSAssetStorage assetStorage;
	private final File cacheDir;
	
	private final Map<UUID, GSSessionTracker> trackers;
	private boolean iteratingTrackers;
	
	public GSSessionManager(GSIServerModuleManager manager, GSAssetStorage assetStorage, File cacheDir) {
		this.manager = manager;
		this.assetStorage = assetStorage;
		this.cacheDir = cacheDir;
	
		trackers = new HashMap<>();
		iteratingTrackers = false;
	}

	public void init() {
		assetStorage.addListener(this);
	}

	public void dispose() {
		stopAll();
		assetStorage.removeListener(this);
	}
	
	public boolean onRequest(ServerPlayerEntity player, GSESessionRequestType requestType, UUID assetUUID) {
		if (assetStorage.hasPermission(player, assetUUID)) {
			GSSessionTracker tracker = getTracker(assetUUID);
			if (tracker != null && tracker.onRequest(player, requestType)) {
				// Remove trackers that do not contain sessions.
				if (requestType == GSESessionRequestType.REQUEST_STOP && tracker.isEmpty()) {
					trackers.remove(assetUUID);
					assetStorage.unloadAsset(assetUUID);
				}
				return true;
			}
		}
		return false;
	}
	
	private GSSessionTracker getTracker(UUID assetUUID) {
		GSSessionTracker tracker = trackers.get(assetUUID);
		if (tracker == null) {
			// Note: Synchronous loading of asset. Might be slow.
			GSAbstractAsset asset = assetStorage.requestAsset(assetUUID);
			if (asset != null) {
				tracker = new GSSessionTracker(manager, asset, getCacheDir(assetUUID));
				trackers.put(assetUUID, tracker);
			}
		}
		return tracker;
	}
	
	public void stopAll(ServerPlayerEntity player) {
		iteratingTrackers = true;
		try {
			Iterator<Map.Entry<UUID, GSSessionTracker>> itr = trackers.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<UUID, GSSessionTracker> entry = itr.next();
				UUID assetUUID = entry.getKey();
				GSSessionTracker tracker = entry.getValue();
				tracker.onRequest(player, GSESessionRequestType.REQUEST_STOP);
				if (tracker.isEmpty()) {
					itr.remove();
					assetStorage.unloadAsset(assetUUID);
				}
			}
		} finally {
			iteratingTrackers = false;
		}
	}

	public void stopAll() {
		iteratingTrackers = true;
		try {
			for (GSSessionTracker tracker : trackers.values())
				tracker.stopAll();
			trackers.clear();
			assetStorage.unloadAll();
		} finally {
			iteratingTrackers = false;
		}
	}
	
	public void onDeltasReceived(ServerPlayerEntity player, UUID assetUUID, GSIDelta<GSSession>[] deltas) {
		GSSessionTracker tracker = trackers.get(assetUUID);
		if (tracker != null)
			tracker.onDeltasReceived(player, deltas);
	}
	
	private File getCacheDir(UUID assetUUID) {
		return new File(cacheDir, assetUUID.toString());
	}
	
	@Override
	public void onAssetAdded(UUID assetUUID) {
	}

	@Override
	public void onAssetRemoved(UUID assetUUID) {
		if (!iteratingTrackers) {
			GSSessionTracker tracker = trackers.remove(assetUUID);
			if (tracker != null)
				tracker.stopAll();
		}
	}
}
