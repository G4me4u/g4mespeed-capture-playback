package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAbstractAsset;
import com.g4mesoft.captureplayback.common.asset.GSCompositionAsset;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSSequenceAsset;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionSide;
import com.g4mesoft.captureplayback.session.GSSessionStartPacket;
import com.g4mesoft.captureplayback.session.GSSessionStopPacket;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.util.GSFileUtil;

import net.minecraft.server.network.ServerPlayerEntity;

public class GSSessionTracker implements GSISessionListener {

	private static final String SESSION_EXTENSION = ".session";
	
	private final GSIServerModuleManager manager;
	private final GSAbstractAsset asset;
	private final File cacheDir;
	private final GSESessionType sessionType;
	
	private final Map<UUID, GSSession> playerUUIDToSession;
	private final Map<GSSession, UUID> sessionToPlayerUUID;
	
	GSSessionTracker(GSIServerModuleManager manager, GSAbstractAsset asset, File cacheDir) {
		this.manager = manager;
		this.asset = asset;
		this.cacheDir = cacheDir;
		sessionType = toSessionType(asset.getType());
	
		playerUUIDToSession = new HashMap<>();
		sessionToPlayerUUID = new IdentityHashMap<>();
	}
	
	private static GSESessionType toSessionType(GSEAssetType assetType) {
		switch (assetType) {
		case COMPOSITION:
			return GSESessionType.COMPOSITION;
		case SEQUENCE:
			return GSESessionType.SEQUENCE;
		}
		throw new IllegalStateException("Unknown asset type");
	}
	
	public boolean onRequest(ServerPlayerEntity player, GSESessionRequestType requestType) {
		switch (requestType) {
		case REQUEST_START:
			return onRequestStart(player);
		case REQUEST_STOP:
			return onRequestStop(player);
		}
		throw new IllegalStateException("Unknown request type");
	}
	
	private boolean onRequestStart(ServerPlayerEntity player) {
		UUID playerUUID = player.getUuid();
		if (playerUUIDToSession.containsKey(playerUUID))
			onRequestStop(player);
		GSSession session = readSession(playerUUID);
		if (session == null || !asset.getUUID().equals(session.get(GSSession.ASSET_UUID))) {
			session = new GSSession(sessionType);
			session.set(GSSession.ASSET_UUID, asset.getUUID());
		}
		// The the primary asset of the session
		switch (sessionType) {
		case COMPOSITION:
			session.set(GSSession.COMPOSITION, ((GSCompositionAsset)asset).getComposition());
			break;
		case SEQUENCE:
			session.set(GSSession.SEQUENCE, ((GSSequenceAsset)asset).getSequence());
			break;
		}
		session.setSide(GSSessionSide.SERVER_SIDE);
		session.addListener(this);

		playerUUIDToSession.put(playerUUID, session);
		sessionToPlayerUUID.put(session, playerUUID);
		manager.sendPacket(new GSSessionStartPacket(session), player);
		
		return true;
	}

	private boolean onRequestStop(ServerPlayerEntity player) {
		GSSession session = playerUUIDToSession.remove(player.getUuid());
		if (session != null) {
			sessionToPlayerUUID.remove(session);
			onSessionStopped(player, session);
			return true;
		}
		return false;
	}
	
	private void onSessionStopped(ServerPlayerEntity player, GSSession session) {
		writeSession(player.getUuid(), session);
		session.removeListener(this);
		manager.sendPacket(new GSSessionStopPacket(asset.getUUID()), player);
	}

	public void onDeltasReceived(ServerPlayerEntity player, GSIDelta<GSSession>[] deltas) {
		GSSession session = playerUUIDToSession.get(player.getUuid());
		if (session != null)
			session.applySessionDeltas(deltas);
	}
	
	public void stopAll() {
		for (Map.Entry<UUID, GSSession> entry : playerUUIDToSession.entrySet()) {
			ServerPlayerEntity player = manager.getPlayer(entry.getKey());
			if (player != null)
				onSessionStopped(player, entry.getValue());
		}
		playerUUIDToSession.clear();
		sessionToPlayerUUID.clear();
	}
	
	private GSSession readSession(UUID playerUUID) {
		try {
			return GSFileUtil.readFile(getSessionFile(playerUUID), GSSession::read);
		} catch (IOException ignore) {
		}
		return null;
	}
	
	private void writeSession(UUID playerUUID, GSSession session) {
		try {
			GSFileUtil.writeFile(getSessionFile(playerUUID), session, GSSession::writeCache);
		} catch (IOException ignore) {
		}
	}
	
	private File getSessionFile(UUID playerUUID) {
		return new File(cacheDir, playerUUID.toString() + SESSION_EXTENSION);
	}
	
	public GSESessionType getSessionType() {
		return sessionType;
	}

	public boolean isEmpty() {
		return playerUUIDToSession.isEmpty();
	}
	
	@Override
	public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
		UUID playerUUID = sessionToPlayerUUID.get(session);
		if (playerUUID != null) {
			ServerPlayerEntity player = manager.getPlayer(playerUUID);
			if (player != null)
				manager.sendPacket(new GSSessionDeltasPacket(asset.getUUID(), deltas), player);
		}
	}
}
