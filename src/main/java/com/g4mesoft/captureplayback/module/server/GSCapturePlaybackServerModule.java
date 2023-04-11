package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.UUID;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetHistoryPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoChangedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoRemovedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetManager;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistoryListener;
import com.g4mesoft.captureplayback.common.asset.GSIPlayerCache;
import com.g4mesoft.captureplayback.common.asset.GSIPlayerCacheListener;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCacheEntry;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCacheEntryAddedPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCacheEntryRemovedPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCachePacket;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.server.GSIServerModule;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.packet.GSIPacket;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCapturePlaybackServerModule implements GSIServerModule, GSIAssetHistoryListener,
                                                      GSIPlayerCacheListener {

	public static final String SESSION_CACHE_DIRECTORY = "sessions";
	public static final String COMPOSITION_EXTENSION = ".gcomp";
	
	public static final GSVersion ASSET_STORAGE_VERSION = new GSVersion(0, 4, 2);

	private GSIServerModuleManager manager;

	private GSAssetManager assetManager;
	private GSIAssetHistory assetHistory;
	private GSIPlayerCache playerCache;
	private GSSessionManager sessionManager;
	
	public GSCapturePlaybackServerModule() {
	}
	
	@Override
	public void init(GSIServerModuleManager manager) {
		this.manager = manager;

		assetManager = new GSAssetManager(manager);
		assetManager.init();
		assetHistory = assetManager.getStoredHistory();
		assetHistory.addListener(this);
		playerCache = assetManager.getPlayerCache();
		playerCache.addListener(this);
		sessionManager = new GSSessionManager(manager, assetManager, getSessionDirectory());
		sessionManager.init();
	}
	
	@Override
	public void onClose() {
		sessionManager.dispose();
		sessionManager = null;
		assetHistory.removeListener(this);
		assetHistory = null;
		assetManager.unloadAll();
		assetManager = null;
		
		manager = null;
	}

	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		GSAssetCommand.registerCommand(dispatcher, GSEAssetType.COMPOSITION);
		GSAssetCommand.registerCommand(dispatcher, GSEAssetType.SEQUENCE);
		GSPlaybackCommand.registerCommand(dispatcher);
		GSCaptureCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSExtensionInfo coreInfo) {
		manager.sendPacket(new GSAssetHistoryPacket(assetHistory), player, ASSET_STORAGE_VERSION);
		manager.sendPacket(new GSPlayerCachePacket(playerCache), player, ASSET_STORAGE_VERSION);
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

	public GSAssetManager getAssetManager() {
		return assetManager;
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
	
	@Override
	public void onEntryAdded(UUID playerUUID) {
		GSIPacket packet;
		if (playerUUID == null) {
			// The entire cache has changed...
			packet = new GSPlayerCachePacket(playerCache);
		} else {
			GSPlayerCacheEntry entry = playerCache.get(playerUUID);
			//assert(entry != null)
			packet = new GSPlayerCacheEntryAddedPacket(playerUUID, entry);
		}
		manager.sendPacketToAll(packet, ASSET_STORAGE_VERSION);
	}
	
	@Override
	public void onEntryRemoved(UUID playerUUID) {
		GSIPacket packet = new GSPlayerCacheEntryRemovedPacket(playerUUID);
		manager.sendPacketToAll(packet, ASSET_STORAGE_VERSION);
	}
}
