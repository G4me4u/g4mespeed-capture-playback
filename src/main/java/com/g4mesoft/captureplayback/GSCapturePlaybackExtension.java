package com.g4mesoft.captureplayback;

import java.util.IdentityHashMap;
import java.util.Map;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.GSExtensionUID;
import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.module.GSSequenceDeltaPacket;
import com.g4mesoft.captureplayback.module.GSSequencePacket;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelDisabledDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelInfoDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelMovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSEntryAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSEntryRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSEntryTimeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSEntryTypeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceNameDelta;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSNoteBlockSignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSPistonSignalEventHandler;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.registry.GSSupplierRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class GSCapturePlaybackExtension implements GSIExtension {

	public static final String NAME = "Capture & Playback";
	/* "CAPL" in ASCII as HEX */
	public static final GSExtensionUID UID = new GSExtensionUID(0x4341504C);
	public static final GSVersion VERSION = new GSVersion(0, 1, 1);
	
	public static final GSExtensionInfo INFO = new GSExtensionInfo(NAME, UID, VERSION);
	
	private static final String TRANSLATION_PATH = "/assets/g4mespeed/captureplayback/lang/en.lang";
	
	private GSSupplierRegistry<Integer, GSISequenceDelta> deltaRegistry;
	private Map<Block, GSISignalEventHandler> signalEventHandlerRegistry;
	
	@Environment(EnvType.CLIENT)
	private GSCapturePlaybackModule clientModule;
	private GSCapturePlaybackModule serverModule;
	
	@Override
	public void init() {
		deltaRegistry = new GSSupplierRegistry<>();
		
		deltaRegistry.register(0, GSSequenceNameDelta.class, GSSequenceNameDelta::new);
		deltaRegistry.register(1, GSChannelAddedDelta.class, GSChannelAddedDelta::new);
		deltaRegistry.register(2, GSChannelRemovedDelta.class, GSChannelRemovedDelta::new);
		deltaRegistry.register(3, GSChannelInfoDelta.class, GSChannelInfoDelta::new);
		deltaRegistry.register(4, GSChannelDisabledDelta.class, GSChannelDisabledDelta::new);
		deltaRegistry.register(5, GSEntryAddedDelta.class, GSEntryAddedDelta::new);
		deltaRegistry.register(6, GSEntryRemovedDelta.class, GSEntryRemovedDelta::new);
		deltaRegistry.register(7, GSEntryTimeDelta.class, GSEntryTimeDelta::new);
		deltaRegistry.register(8, GSEntryTypeDelta.class, GSEntryTypeDelta::new);
		deltaRegistry.register(9, GSChannelMovedDelta.class, GSChannelMovedDelta::new);
		
		signalEventHandlerRegistry = new IdentityHashMap<>();
		
		signalEventHandlerRegistry.put(Blocks.PISTON, new GSPistonSignalEventHandler());
		signalEventHandlerRegistry.put(Blocks.STICKY_PISTON, new GSPistonSignalEventHandler());
		signalEventHandlerRegistry.put(Blocks.NOTE_BLOCK, new GSNoteBlockSignalEventHandler());
	}
	
	@Override
	public void registerPackets(GSSupplierRegistry<Integer, GSIPacket> registry) {
		registry.register(0, GSSequencePacket.class, GSSequencePacket::new);
		registry.register(1, GSSequenceDeltaPacket.class, GSSequenceDeltaPacket::new);
	}
	
	@Override
	public void addClientModules(GSControllerClient controller) {
		controller.addModule(getClientModule());
	}

	@Override
	public void addServerModules(GSControllerServer controller) {
		controller.addModule(getServerModule());
	}
	
	@Override
	public String getTranslationPath() {
		return TRANSLATION_PATH;
	}
	
	@Override
	public GSExtensionInfo getInfo() {
		return INFO;
	}
	
	@Environment(EnvType.CLIENT)
	public GSCapturePlaybackModule getClientModule() {
		if (clientModule == null)
			clientModule = new GSCapturePlaybackModule();
		return clientModule;
	}

	public GSCapturePlaybackModule getServerModule() {
		if (serverModule == null)
			serverModule = new GSCapturePlaybackModule();
		return serverModule;
	}
	
	public GSSupplierRegistry<Integer, GSISequenceDelta> getDeltaRegistry() {
		return deltaRegistry;
	}
	
	public Map<Block, GSISignalEventHandler> getSignalEventHandlerRegistry() {
		return signalEventHandlerRegistry;
	}
}
