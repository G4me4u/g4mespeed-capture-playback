package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
import com.g4mesoft.captureplayback.common.asset.GSIAssetStorageListener;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.server.GSIServerModuleManager;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionManager implements GSIAssetStorageListener {

	private final GSIServerModuleManager manager;
	private final GSAssetManager assetManager;
	private final File cacheDir;
	
	private final Map<UUID, GSSessionTracker> trackers;
	private final Map<UUID, Set<UUID>> playerToAssets;
	private final GSSessionTrackerListener trackerListener;

	private final List<GSISessionStatusListener> listeners;
	
	public GSSessionManager(GSIServerModuleManager manager, GSAssetManager assetManager, File cacheDir) {
		this.manager = manager;
		this.assetManager = assetManager;
		this.cacheDir = cacheDir;
	
		trackers = new HashMap<>();
		playerToAssets = new HashMap<>();
		trackerListener = new GSSessionTrackerListener();

		listeners = new ArrayList<>();
	}

	public void init() {
		assetManager.addListener(this);
	}

	public void dispose() {
		stopAll();
		assetManager.removeListener(this);
	}
	
	public boolean onRequest(ServerPlayerEntity player, GSESessionRequestType requestType, UUID assetUUID) {
		if (assetManager.hasPermission(player, assetUUID)) {
			GSSessionTracker tracker = getTracker(assetUUID);
			if (tracker != null && tracker.onRequest(player, requestType)) {
				// Remove trackers that do not contain sessions.
				if (requestType == GSESessionRequestType.REQUEST_STOP && tracker.isEmpty()) {
					trackers.remove(assetUUID);
					tracker.getRef().release();
					onTrackerRemoved(tracker);
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
			GSAssetRef ref = assetManager.requestAsset(assetUUID);
			GSAssetInfo info = assetManager.getInfo(assetUUID);
			if (info != null && ref != null) {
				tracker = new GSSessionTracker(manager, info, ref, getCacheDir(assetUUID));
				trackers.put(assetUUID, tracker);
				onTrackerAdded(tracker);
			}
		}
		return tracker;
	}
	
	public void stopAll(ServerPlayerEntity player) {
		Set<UUID> assetUUIDs = playerToAssets.get(player.getUuid());
		if (assetUUIDs != null) {
			// Ensure we make a copy to avoid concurrent modification...
			UUID[] assetUUIDArray = assetUUIDs.toArray(new UUID[0]);
			for (UUID assetUUID : assetUUIDArray)
				onRequest(player, GSESessionRequestType.REQUEST_STOP, assetUUID);
		}
	}

	public void stopAll() {
		for (GSSessionTracker tracker : trackers.values()) {
			tracker.stopAll();
			onTrackerRemoved(tracker);
		}
		trackers.clear();
	}
	
	public Iterator<GSSession> iterateSessions(ServerPlayerEntity player) {
		return new GSPlayerSessionIterator(player);
	}
	
	private void onTrackerAdded(GSSessionTracker tracker) {
		tracker.setListener(trackerListener);
	}

	private void onTrackerRemoved(GSSessionTracker tracker) {
		tracker.setListener(null);
	}
	
	public void onDeltasReceived(ServerPlayerEntity player, UUID assetUUID, GSIDelta<GSSession>[] deltas) {
		GSSessionTracker tracker = trackers.get(assetUUID);
		if (tracker != null)
			tracker.onDeltasReceived(player, deltas);
	}
	
	private File getCacheDir(UUID assetUUID) {
		return new File(cacheDir, assetUUID.toString());
	}

	public void addListener(GSISessionStatusListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	public void removeListener(GSISessionStatusListener listener) {
		listeners.remove(listener);
	}
	
	private void dispatchSessionStarted(ServerPlayerEntity player, UUID assetUUID) {
		for (GSISessionStatusListener listener : listeners)
			listener.sessionStarted(player, assetUUID);
	}

	private void dispatchSessionStopped(ServerPlayerEntity player, UUID assetUUID) {
		for (GSISessionStatusListener listener : listeners)
			listener.sessionStopped(player, assetUUID);
	}
	
	@Override
	public void onAssetAdded(UUID assetUUID) {
	}

	@Override
	public void onAssetRemoved(UUID assetUUID) {
		GSSessionTracker tracker = trackers.remove(assetUUID);
		if (tracker != null) {
			tracker.stopAll();
			onTrackerRemoved(tracker);
		}
	}

	private class GSSessionTrackerListener implements GSISessionStatusListener {

		@Override
		public void sessionStarted(ServerPlayerEntity player, UUID assetUUID) {
			Set<UUID> assetUUIDs = playerToAssets.get(player.getUuid());
			if (assetUUIDs == null) {
				assetUUIDs = new LinkedHashSet<>();
				playerToAssets.put(player.getUuid(), assetUUIDs);
			}
			if (assetUUIDs.add(assetUUID))
				dispatchSessionStarted(player, assetUUID);
		}

		@Override
		public void sessionStopped(ServerPlayerEntity player, UUID assetUUID) {
			Set<UUID> assetUUIDs = playerToAssets.get(player.getUuid());
			if (assetUUIDs != null && assetUUIDs.remove(assetUUID)) {
				if (assetUUIDs.isEmpty())
					playerToAssets.remove(player.getUuid());
				dispatchSessionStopped(player, assetUUID);
			}
		}
	}
	
	private class GSPlayerSessionIterator implements Iterator<GSSession> {

		private final ServerPlayerEntity player;
		private final Iterator<UUID> itr;
		
		public GSPlayerSessionIterator(ServerPlayerEntity player) {
			this.player = player;
			Set<UUID> assetUUIDs = playerToAssets.get(player.getUuid());
			itr = (assetUUIDs != null) ? assetUUIDs.iterator() : Collections.emptyIterator();
		}
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public GSSession next() {
			GSSessionTracker tracker = trackers.get(itr.next());
			if (tracker == null)
				throw new ConcurrentModificationException();
			GSSession session = tracker.getSession(player);
			if (session == null)
				throw new ConcurrentModificationException();
			return session;
		}
	}
}
