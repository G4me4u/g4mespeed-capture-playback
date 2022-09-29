package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetStorage;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.server.GSIServerModule;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCapturePlaybackServerModule implements GSIServerModule {

	public static final String ASSET_DIRECTORY = "assets";
	public static final String SESSION_CACHE_DIRECTORY = "sessions";
	public static final String COMPOSITION_EXTENSION = ".gcomp";

	private GSIServerModuleManager manager;

	private GSAssetStorage assetStorage;
	private GSSessionManager sessionManager;
	
	public GSCapturePlaybackServerModule() {
	}
	
	@Override
	public void init(GSIServerModuleManager manager) {
		this.manager = manager;

		assetStorage = new GSAssetStorage(getAssetDirectory());
		assetStorage.loadStoredHistory();
		sessionManager = new GSSessionManager(manager, assetStorage, getSessionDirectory());
		sessionManager.init();
	}
	
	@Override
	public void onClose() {
		sessionManager.dispose();
		sessionManager = null;
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
}
