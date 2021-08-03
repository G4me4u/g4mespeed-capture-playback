package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionDelta;
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
	
	public void removeComposition(UUID compositionUUID) {
		GSSessionTracker tracker = sessionTrackers.remove(compositionUUID);
		
		if (tracker != null) {
			tracker.stopAllSessions();
			tracker.uninstall();
		}
	}
	
	public void onSessionRequest(ServerPlayerEntity player, GSESessionType sessionType, GSESessionRequestType requestType, UUID structureUUID) {
		switch (sessionType) {
		case COMPOSITION:
			if (requestType == GSESessionRequestType.REQUEST_START) {
				startCompositionSession(player, structureUUID);
			} else {
				stopCompositionSession(player);
			}
			break;
		case SEQUENCE:
			if (requestType == GSESessionRequestType.REQUEST_START) {
				startSequenceSession(player, structureUUID);
			} else {
				stopSequenceSession(player);
			}
			break;
		}
	}

	private void startCompositionSession(ServerPlayerEntity player, UUID compositionUUID) {
		if (playerToTracker.containsKey(player.getUuid()))
			stopCompositionSession(player);
		
		GSSessionTracker tracker = sessionTrackers.get(compositionUUID);
		if (tracker != null && tracker.startCompositionSession(player))
			playerToTracker.put(player.getUuid(), tracker);
	}
	
	private void stopCompositionSession(ServerPlayerEntity player) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null) {
			tracker.stopCompositionSession(player);
			playerToTracker.remove(player.getUuid());
		}
	}

	private void startSequenceSession(ServerPlayerEntity player, UUID trackUUID) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.startSequenceSession(player, trackUUID);
	}
	
	private void stopSequenceSession(ServerPlayerEntity player) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.stopSequenceSession(player);
	}

	public void stopAllSessions(ServerPlayerEntity player) {
		GSSessionTracker tracker = playerToTracker.remove(player.getUuid());
		if (tracker != null) {
			tracker.stopSequenceSession(player);
			tracker.stopCompositionSession(player);
		}
	}
	
	public void stopAllSessions() {
		for (GSSessionTracker tracker : sessionTrackers.values())
			tracker.stopAllSessions();
		playerToTracker.clear();
	}
	
	public void onDeltasReceived(ServerPlayerEntity player, GSESessionType sessionType, GSISessionDelta[] deltas) {
		GSSessionTracker tracker = playerToTracker.get(player.getUuid());
		if (tracker != null)
			tracker.onDeltasReceived(player, sessionType, deltas);
	}
}
