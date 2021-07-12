package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.core.server.GSIServerModuleManager;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionManager {

	private static final String SESSION_CACHE_DIRECTORY_NAME = "sessions";
	
	private final GSIServerModuleManager manager;
	
	private final File sessionCacheDir;

	private final Map<UUID, GSSessionTracker> sessionTrackers;
	private final Map<UUID, GSSessionTracker> playerToTracker;
	
	public GSSessionManager(GSIServerModuleManager manager) {
		this.manager = manager;
		
		sessionCacheDir = new File(manager.getCacheFile(), SESSION_CACHE_DIRECTORY_NAME);
		
		sessionTrackers = new HashMap<>();
		playerToTracker = new HashMap<>();
	}
	
	public void addComposition(GSComposition composition) {
		UUID compositionUUID = composition.getCompositionUUID();
		
		if (!sessionTrackers.containsKey(compositionUUID)) {
			File cacheDir = new File(sessionCacheDir, compositionUUID.toString());
			GSSessionTracker tracker = new GSSessionTracker(manager, composition, cacheDir);
			tracker.install();
			
			sessionTrackers.put(compositionUUID, tracker);
		}
	}
	
	public void startCompositionSession(ServerPlayerEntity player, UUID compositionUUID) {
		if (!playerToTracker.containsKey(player.getUuid())) {
			GSSessionTracker tracker = sessionTrackers.get(compositionUUID);
			if (tracker != null && tracker.startCompositionSession(player))
				playerToTracker.put(player.getUuid(), tracker);
		}
	}

	public void stopCompositionSession(ServerPlayerEntity player) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null) {
			tracker.stopCompositionSession(player);
			playerToTracker.remove(player.getUuid());
		}
	}

	public void startSequenceSession(ServerPlayerEntity player, UUID trackUUID) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.startSequenceSession(player, trackUUID);
	}
	
	public void stopSequenceSession(ServerPlayerEntity player) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.stopSequenceSession(player);
	}
	
	public void stopAllSessions() {
		for (GSSessionTracker tracker : sessionTrackers.values())
			tracker.stopAllSessions();
		playerToTracker.clear();
	}
	
	public void onDeltaReceived(ServerPlayerEntity player, GSICompositionDelta delta) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.onDeltaReceived(player, delta);
	}

	public void onCompositionSessionChanged(ServerPlayerEntity player, GSCompositionSession session) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.onCompositionSessionChanged(player, session);
	}

	public void onSequenceSessionChanged(ServerPlayerEntity player, GSSequenceSession session) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.onSequenceSessionChanged(player, session);
	}
}
