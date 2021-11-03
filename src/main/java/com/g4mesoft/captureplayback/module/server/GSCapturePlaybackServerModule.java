package com.g4mesoft.captureplayback.module.server;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.GSCoreExtension;
import com.g4mesoft.core.server.GSIServerModule;
import com.g4mesoft.core.server.GSIServerModuleManager;
import com.g4mesoft.util.GSBufferUtil;
import com.g4mesoft.util.GSFileUtil;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GSCapturePlaybackServerModule implements GSIServerModule {

	public static final String COMPOSITION_DIRECTORY = "compositions";
	public static final String HISTORY_FILE_NAME = "history.txt";
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

		try {
			compositionFileName = readCompositionHistory();
			composition = readComposition(compositionFileName);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to read composition!");
			compositionFileName = DEFAULT_COMPOSITION_FILE_NAME;
			composition = new GSComposition(UUID.randomUUID(), compositionFileName);
		}

		sessionManager = new GSSessionManager(manager);
		sessionManager.addComposition(composition);
	}
	
	@Override
	public void onClose() {
		try {
			writeCompositionHistory(compositionFileName);
			writeComposition(composition, compositionFileName);
		} catch (IOException e) {
			CapturePlaybackMod.GSCP_LOGGER.warn("Unable to write composition!");
		}
		
		sessionManager.stopAllSessions();
		sessionManager = null;
		
		manager = null;
	}

	private String readCompositionHistory() throws IOException {
		return GSFileUtil.readFile(getHistoryFile(), buf -> {
			return buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		});
	}

	private void writeCompositionHistory(String history) throws IOException {
		GSFileUtil.writeFile(getHistoryFile(), history, PacketByteBuf::writeString);
	}
	
	private File getHistoryFile() {
		return new File(getCompositionDirectory(), HISTORY_FILE_NAME);
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
		GSCaptureCommand.registerCommand(dispatcher);
		GSSequenceCommand.registerCommand(dispatcher);
		GSCompositionCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onPlayerLeave(ServerPlayerEntity player) {
		sessionManager.stopAllSessions(player);
	}
	
	public void onSessionRequest(ServerPlayerEntity player, GSESessionType sessionType, GSESessionRequestType requestType, UUID structureUUID) {
		if (sessionType == GSESessionType.COMPOSITION) {
			// TODO: fix this, such that the client knows what compositions are available
			structureUUID = composition.getCompositionUUID();
		}
		sessionManager.onSessionRequest(player, sessionType, requestType, structureUUID);
	}

	public void onSessionDeltasReceived(ServerPlayerEntity player, GSESessionType sessionType, GSIDelta<GSSession>[] deltas) {
		sessionManager.onDeltasReceived(player, sessionType, deltas);
	}
	
	private File getCompositionFile(String fileName) {
		return new File(getCompositionDirectory(), fileName + COMPOSITION_EXTENSION);
	}

	public File getCompositionDirectory() {
		return new File(manager.getCacheFile(), COMPOSITION_DIRECTORY);
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
			if (manager.isExtensionInstalled(player, GSCoreExtension.UID)) {
				sessionManager.onSessionRequest(player, GSESessionType.COMPOSITION,
						GSESessionRequestType.REQUEST_START, composition.getCompositionUUID());
			}
		}
	}

	public void startSequenceSessionForAll(UUID trackUUID) {
		for (ServerPlayerEntity player : manager.getAllPlayers()) {
			sessionManager.onSessionRequest(player, GSESessionType.SEQUENCE,
					GSESessionRequestType.REQUEST_START, trackUUID);
		}
	}
}
