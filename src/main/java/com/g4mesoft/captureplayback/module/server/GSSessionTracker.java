package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetRef;
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
	private final GSAssetInfo info;
	private final GSAssetRef ref;
	private final File cacheDir;
	private final GSESessionType sessionType;
	
	private final Map<UUID, GSSession> playerUUIDToSession;
	private final Map<GSSession, UUID> sessionToPlayerUUID;
	
	private GSISessionStatusListener listener;
	
	GSSessionTracker(GSIServerModuleManager manager, GSAssetInfo info, GSAssetRef ref, File cacheDir) {
		if (!info.getAssetUUID().equals(ref.get().getUUID()))
			throw new IllegalArgumentException("Asset info does not correspond to the asset.");
		this.manager = manager;
		this.info = info;
		this.ref = ref;
		this.cacheDir = cacheDir;
		sessionType = toSessionType(info.getType());
	
		playerUUIDToSession = new HashMap<>();
		sessionToPlayerUUID = new IdentityHashMap<>();

		listener = null;
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
		if (session == null || !info.getAssetUUID().equals(session.get(GSSession.ASSET_UUID))) {
			session = new GSSession(sessionType);
			session.set(GSSession.ASSET_UUID, info.getAssetUUID());
			session.set(GSSession.ASSET_HANDLE, info.getHandle());
		}
		// The primary asset of the session
		switch (sessionType) {
		case COMPOSITION:
			session.set(GSSession.COMPOSITION, ((GSCompositionAsset)ref.get()).getComposition());
			break;
		case SEQUENCE:
			session.set(GSSession.SEQUENCE, ((GSSequenceAsset)ref.get()).getSequence());
			break;
		}
		session.setSide(GSSessionSide.SERVER_SIDE);

		playerUUIDToSession.put(playerUUID, session);
		sessionToPlayerUUID.put(session, playerUUID);
		onSessionStarted(player, session);
		
		return true;
	}
	
	private void onSessionStarted(ServerPlayerEntity player, GSSession session) {
		session.addListener(this);
		manager.sendPacket(new GSSessionStartPacket(session), player);
		dispatchSessionStarted(player, info.getAssetUUID());
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
		manager.sendPacket(new GSSessionStopPacket(info.getAssetUUID()), player);
		dispatchSessionStopped(player, info.getAssetUUID());
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
	
	public GSSession getSession(ServerPlayerEntity player) {
		return playerUUIDToSession.get(player.getUuid());
	}
	
	public void setListener(GSISessionStatusListener listener) {
		if (this.listener != null && listener != null)
			throw new IllegalStateException("Tracker only supports a single listener");
		this.listener = listener;
	}

	private void dispatchSessionStarted(ServerPlayerEntity player, UUID assetUUID) {
		if (listener != null)
			listener.sessionStarted(player, assetUUID);
	}

	private void dispatchSessionStopped(ServerPlayerEntity player, UUID assetUUID) {
		if (listener != null)
			listener.sessionStopped(player, assetUUID);
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

	public GSAssetRef getRef() {
		return ref;
	}
	
	@Override
	public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
		UUID playerUUID = sessionToPlayerUUID.get(session);
		if (playerUUID != null) {
			ServerPlayerEntity player = manager.getPlayer(playerUUID);
			if (player != null)
				manager.sendPacket(new GSSessionDeltasPacket(info.getAssetUUID(), deltas), player);
		}
	}
}
