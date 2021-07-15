package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.core.GSCoreExtension;
import com.g4mesoft.core.server.GSIServerModule;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.util.GSFileUtil;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCapturePlaybackServerModule implements GSIServerModule {

	public static final String DEFAULT_COMPOSITION_FILE_NAME = "default_composition";
	public static final String COMPOSITION_EXTENSION = ".gcomp";

	private GSIServerModuleManager manager;

	private GSComposition composition;
	private String compositionFileName;
	private GSSessionManager sessionManager;
	
	public GSCapturePlaybackServerModule() {
	}
	
	@Override
	public void init(GSIServerModuleManager manager) {
		this.manager = manager;

		compositionFileName = DEFAULT_COMPOSITION_FILE_NAME;
		
		try {
			composition = readComposition(DEFAULT_COMPOSITION_FILE_NAME);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to read default composition!");
			composition = new GSComposition(UUID.randomUUID(), compositionFileName);
		}

		sessionManager = new GSSessionManager(manager);
		sessionManager.addComposition(composition);
	}
	
	@Override
	public void onClose() {
		try {
			writeComposition(composition, DEFAULT_COMPOSITION_FILE_NAME);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to write default composition!");
		}
		
		sessionManager.stopAllSessions();
		sessionManager = null;
		
		manager = null;
	}

	public GSComposition readComposition(String fileName) throws IOException {
		return GSFileUtil.readFile(getCompositionFile(fileName), GSComposition::read);
	}
	
	public void writeComposition(GSComposition composition, String fileName) throws IOException {
		GSFileUtil.writeFile(getCompositionFile(fileName), composition, GSComposition::write);
	}
	
	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		GSPlaybackCommand.registerCommand(dispatcher);
		//GSCaptureCommand.registerCommand(dispatcher);
		GSSequenceCommand.registerCommand(dispatcher);
		GSCompositionCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSExtensionInfo coreInfo) {
		sessionManager.startCompositionSession(player, composition.getCompositionUUID());
	}
	
	@Override
	public void onPlayerLeave(ServerPlayerEntity player) {
		sessionManager.stopCompositionSession(player);
	}
	
	public void onSequenceSessionRequest(UUID trackUUID, ServerPlayerEntity player) {
		sessionManager.startSequenceSession(player, trackUUID);
	}
	
	public void onDeltaReceived(GSICompositionDelta delta, ServerPlayerEntity player) {
		sessionManager.onDeltaReceived(player, delta);
	}
	
	public void onCompositionSessionChanged(GSCompositionSession session, ServerPlayerEntity player) {
		sessionManager.onCompositionSessionChanged(player, session);
	}
	
	public void onSequenceSessionChanged(GSSequenceSession session, ServerPlayerEntity player) {
		sessionManager.onSequenceSessionChanged(player, session);
	}
	
	private File getCompositionFile(String fileName) {
		return new File(getCompositionDirectory(), fileName + COMPOSITION_EXTENSION);
	}

	public File getCompositionDirectory() {
		return manager.getCacheFile();
	}

	public GSComposition getComposition() {
		return composition;
	}
	
	/* Methods for loading and writing compositions. TODO: remove this */
	
	public boolean writeComposition() {
		try {
			writeComposition(composition, compositionFileName);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	public boolean createComposition(String fileName) {
		setComposition(new GSComposition(UUID.randomUUID(), fileName), fileName);
		return true;
	}

	public boolean setComposition(String fileName) {
		try {
			setComposition(readComposition(fileName), fileName);
		} catch (IOException ignore) {
			return false;
		}
		
		return true;
	}
	
	public void setComposition(GSComposition composition, String fileName) {
		sessionManager.stopAllSessions();
		sessionManager.removeComposition(this.composition.getCompositionUUID());
		
		this.composition = composition;
		
		compositionFileName = fileName;
		
		sessionManager.addComposition(composition);
		for (ServerPlayerEntity player : manager.getAllPlayers()) {
			if (manager.isExtensionInstalled(player, GSCoreExtension.UID))
				sessionManager.startCompositionSession(player, composition.getCompositionUUID());
		}
	}

	public void startSequenceSessionForAll(UUID trackUUID) {
		for (ServerPlayerEntity player : manager.getAllPlayers())
			sessionManager.startSequenceSession(player, trackUUID);
	}
}
