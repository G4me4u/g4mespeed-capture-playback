package com.g4mesoft.captureplayback.module.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSCreateAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSDecodedAssetFile;
import com.g4mesoft.captureplayback.common.asset.GSDeleteAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSImportAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCache;
import com.g4mesoft.captureplayback.common.asset.GSRequestAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSUnmodifiableAssetHistory;
import com.g4mesoft.captureplayback.gui.GSCompositionEditPanel;
import com.g4mesoft.captureplayback.gui.GSSequenceEditPanel;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionRequestPacket;
import com.g4mesoft.captureplayback.session.GSSessionSide;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.client.GSIClientModuleManager;
import com.g4mesoft.ui.panel.GSPanel;

import net.minecraft.entity.player.PlayerEntity;

public class GSClientAssetManager implements GSISessionListener {

	private final GSIClientModuleManager manager;

	private final Map<UUID, GSSession> sessions;
	private final Map<GSESessionType, GSSession> sessionByType;
	private final Map<GSESessionType, GSPanel> sessionPanels;

	private final GSIAssetHistory history;
	private final GSIAssetHistory unmodifiableHistory;
	
	private final Map<UUID, Consumer<GSDecodedAssetFile>> requestCallbacks;
	
	private final GSPlayerCache playerCache;
	
	/* Visible for GSCapturePlaybackClientModule */
	GSClientAssetManager(GSIClientModuleManager manager) {
		this.manager = manager;

		sessions = new HashMap<>();
		sessionByType = new EnumMap<>(GSESessionType.class);
		sessionPanels = new EnumMap<>(GSESessionType.class);
		
		history = new GSAssetHistory();
		unmodifiableHistory = new GSUnmodifiableAssetHistory(history);
		
		requestCallbacks = new HashMap<>();
	
		playerCache = new GSPlayerCache();
	}
	
	/* Visible for GSCapturePlaybackClientModule */
	void onDisconnect() {
		history.clear();
		UUID[] assetUUIDs = sessions.keySet().toArray(new UUID[0]);
		for (UUID assetUUID : assetUUIDs)
			onSessionStop(assetUUID);
	}
	
	public void requestSession(GSESessionRequestType requestType, UUID assetUUID) {
		manager.sendPacket(new GSSessionRequestPacket(requestType, assetUUID));
	}

	public void requestSession(GSESessionRequestType requestType, GSAssetHandle handle) {
		GSAssetInfo info = history.getFromHandle(handle);
		if (info != null)
			requestSession(requestType, info.getAssetUUID());
	}
	
	public GSSession getSession(GSESessionType sessionType) {
		return sessionByType.get(sessionType);
	}

	public void createAsset(String name, GSEAssetType type, GSAssetHandle handle, UUID originalAssetUUID) {
		if (originalAssetUUID == null || hasPermission(originalAssetUUID))
			manager.sendPacket(new GSCreateAssetPacket(name, type, handle, originalAssetUUID));
	}
	
	public void deleteAsset(UUID assetUUID) {
		if (hasPermission(assetUUID))
			manager.sendPacket(new GSDeleteAssetPacket(assetUUID));
	}
	
	public void importAsset(String name, GSAssetHandle handle, GSDecodedAssetFile assetFile) {
		manager.sendPacket(new GSImportAssetPacket(name, handle, assetFile));
	}
	
	public void requestAsset(UUID assetUUID, Consumer<GSDecodedAssetFile> callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback is null");
		if (hasPermission(assetUUID)) {
			requestCallbacks.put(assetUUID, callback);
			manager.sendPacket(new GSRequestAssetPacket(assetUUID));
		} else {
			// Indicate access denied.
			callback.accept(null);
		}
	}
	
	public boolean hasPermission(UUID assetUUID) {
		GSAssetInfo info = history.get(assetUUID);
		PlayerEntity player = GSClientController.getInstance().getPlayer();
		return info != null && info.hasPermission(player);
	}
	
	public void onSessionStart(GSSession session) {
		GSSession activeSession = getSession(session.getType());
		if (activeSession != null) {
			// Notify the server that we are stopping the active session
			UUID assetUUID = activeSession.get(GSSession.ASSET_UUID);
			requestSession(GSESessionRequestType.REQUEST_STOP, assetUUID);
			onSessionStop(assetUUID);
		}
		
		sessions.put(session.get(GSSession.ASSET_UUID), session);
		sessionByType.put(session.getType(), session);
		session.setSide(GSSessionSide.CLIENT_SIDE);
		session.addListener(this);
	
		switch (session.getType()) {
		case COMPOSITION:
			openSessionPanel(session.getType(), new GSCompositionEditPanel(session));
			break;
		case SEQUENCE:
			openSessionPanel(session.getType(), new GSSequenceEditPanel(session));
			break;
		case PLAYLIST:
			// TODO: update the playlist ui
			break;
		}
	}

	public void onSessionStop(UUID assetUUID) {
		GSSession session = sessions.remove(assetUUID);
		if (session != null) {
			sessionByType.remove(session.getType());
			session.removeListener(this);
			closeSessionPanel(session.getType());
		}
	}
	
	private void openSessionPanel(GSESessionType sessionType, GSPanel panel) {
		if (sessionPanels.containsKey(sessionType))
			closeSessionPanel(sessionType);
		sessionPanels.put(sessionType, panel);
		
		GSClientController.getInstance().getPrimaryGUI().setContent(panel);
	}
	
	private void closeSessionPanel(GSESessionType sessionType) {
		GSPanel panel = sessionPanels.remove(sessionType);
		if (panel != null)
			GSClientController.getInstance().getPrimaryGUI().removeHistory(panel);
	}
	
	@Override
	public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
		manager.sendPacket(new GSSessionDeltasPacket(session.get(GSSession.ASSET_UUID), deltas));
	}
	
	public void onSessionDeltasReceived(UUID assetUUID, GSIDelta<GSSession>[] deltas) {
		GSSession session = sessions.get(assetUUID);
		if (session != null)
			session.applySessionDeltas(deltas);
	}

	public void onAssetHistoryReceived(GSAssetHistory history) {
		this.history.set(history);
	}

	public void onAssetInfoChanged(GSAssetInfo info) {
		history.add(info);
	}

	public void onAssetInfoRemoved(UUID assetUUID) {
		history.remove(assetUUID);
	}

	public void onAssetRequestDenied(UUID assetUUID) {
		Consumer<GSDecodedAssetFile> callback = requestCallbacks.remove(assetUUID);
		if (callback != null)
			callback.accept(null);
	}
	
	public void onAssetRequestSuccess(GSDecodedAssetFile assetFile) {
		UUID assetUUID = assetFile.getAsset().getUUID();
		Consumer<GSDecodedAssetFile> callback = requestCallbacks.remove(assetUUID);
		if (callback != null)
			callback.accept(assetFile);
	}
	
	public GSIAssetHistory getAssetHistory() {
		return unmodifiableHistory;
	}

	public GSPlayerCache getPlayerCache() {
		return playerCache;
	}
}
