package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.UUID;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetHistoryPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoChangedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoRemovedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetStorage;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistoryListener;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.server.GSIServerModule;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.packet.GSIPacket;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCapturePlaybackServerModule implements GSIServerModule, GSIAssetHistoryListener {

	public static final String ASSET_DIRECTORY = "assets";
	public static final String SESSION_CACHE_DIRECTORY = "sessions";
	public static final String COMPOSITION_EXTENSION = ".gcomp";
	
	public static final GSVersion ASSET_STORAGE_VERSION = new GSVersion(0, 4, 2);

	private GSIServerModuleManager manager;

	private GSAssetStorage assetStorage;
	private GSIAssetHistory assetHistory;
	private GSSessionManager sessionManager;
	
	public GSCapturePlaybackServerModule() {
	}
	
	@Override
	public void init(GSIServerModuleManager manager) {
		this.manager = manager;

		assetStorage = new GSAssetStorage(getAssetDirectory());
		assetStorage.loadStoredHistory();
		assetHistory = assetStorage.getStoredHistory();
		assetHistory.addListener(this);
		sessionManager = new GSSessionManager(manager, assetStorage, getSessionDirectory());
		sessionManager.init();
	}
	
	@Override
	public void onClose() {
		sessionManager.dispose();
		sessionManager = null;
		assetHistory.removeListener(this);
		assetHistory = null;
		assetStorage.unloadAll();
		assetStorage = null;
		
		manager = null;
	}

	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		GSAssetCommands.registerCommands(dispatcher);
		GSPlaybackCommand.registerCommand(dispatcher);
		GSCaptureCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSExtensionInfo coreInfo) {
		manager.sendPacket(new GSAssetHistoryPacket(assetHistory), player, ASSET_STORAGE_VERSION);
	}
	
	@Override
	public void onPlayerLeave(ServerPlayerEntity player) {
		sessionManager.stopAll(player);
	}
	
	public boolean onSessionRequest(ServerPlayerEntity player, GSESessionRequestType requestType, UUID assetUUID) {
		return sessionManager.onRequest(player, requestType, assetUUID);
	}

	public void onSessionDeltasReceived(ServerPlayerEntity player, UUID assetUUID, GSIDelta<GSSession>[] deltas) {
		sessionManager.onDeltasReceived(player, assetUUID, deltas);
	}
	
	private File getSessionDirectory() {
		return new File(manager.getCacheFile(), SESSION_CACHE_DIRECTORY);
	}

	private File getAssetDirectory() {
		return new File(manager.getCacheFile(), ASSET_DIRECTORY);
	}

	public GSAssetStorage getAssetStorage() {
		return assetStorage;
	}
	
	public GSSessionManager getSessionManager() {
		return sessionManager;
	}

	@Override
	public void onHistoryChanged(UUID assetUUID) {
		GSIPacket packet;
		if (assetUUID == null) {
			// The entire history has changed...
			packet = new GSAssetHistoryPacket(assetHistory);
		} else {
			GSAssetInfo info = assetHistory.get(assetUUID);
			if (info == null) {
				packet = new GSAssetInfoRemovedPacket(assetUUID);
			} else {
				packet = new GSAssetInfoChangedPacket(info);
			}
		}
		manager.sendPacketToAll(packet, ASSET_STORAGE_VERSION);
	}
}
