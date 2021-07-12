package com.g4mesoft.captureplayback;

import java.util.IdentityHashMap;
import java.util.Map;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.GSExtensionUID;
import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSGroupRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackColorDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryAddedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryOffsetDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackEntryRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackGroupDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackNameDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackRemovedDelta;
import com.g4mesoft.captureplayback.composition.delta.GSTrackSequenceDelta;
import com.g4mesoft.captureplayback.module.GSCompositionDeltaPacket;
import com.g4mesoft.captureplayback.module.GSCompositionSessionChangedPacket;
import com.g4mesoft.captureplayback.module.GSResetCompositionPacket;
import com.g4mesoft.captureplayback.module.GSSequenceSessionChangedPacket;
import com.g4mesoft.captureplayback.module.GSSequenceSessionRequestPacket;
import com.g4mesoft.captureplayback.module.GSStartCompositionSessionPacket;
import com.g4mesoft.captureplayback.module.GSStartSequenceSessionPacket;
import com.g4mesoft.captureplayback.module.GSStopCompositionSessionPacket;
import com.g4mesoft.captureplayback.module.GSStopSequenceSessionPacket;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelDisabledDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryAddedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryTimeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelEntryTypeDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelInfoDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelMovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSChannelRemovedDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceNameDelta;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSNoteBlockSignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSPistonSignalEventHandler;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
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
	
	private GSSupplierRegistry<Integer, GSISequenceDelta> sequenceDeltaRegistry;
	private GSSupplierRegistry<Integer, GSICompositionDelta> compositionDeltaRegistry;
	private Map<Block, GSISignalEventHandler> signalEventHandlerRegistry;
	
	@Environment(EnvType.CLIENT)
	private GSCapturePlaybackClientModule clientModule;
	private GSCapturePlaybackServerModule serverModule;
	
	@Override
	public void init() {
		sequenceDeltaRegistry = new GSSupplierRegistry<>();
		
		sequenceDeltaRegistry.register(0, GSSequenceNameDelta.class, GSSequenceNameDelta::new);
		sequenceDeltaRegistry.register(1, GSChannelAddedDelta.class, GSChannelAddedDelta::new);
		sequenceDeltaRegistry.register(2, GSChannelRemovedDelta.class, GSChannelRemovedDelta::new);
		sequenceDeltaRegistry.register(3, GSChannelInfoDelta.class, GSChannelInfoDelta::new);
		sequenceDeltaRegistry.register(4, GSChannelDisabledDelta.class, GSChannelDisabledDelta::new);
		sequenceDeltaRegistry.register(5, GSChannelEntryAddedDelta.class, GSChannelEntryAddedDelta::new);
		sequenceDeltaRegistry.register(6, GSChannelEntryRemovedDelta.class, GSChannelEntryRemovedDelta::new);
		sequenceDeltaRegistry.register(7, GSChannelEntryTimeDelta.class, GSChannelEntryTimeDelta::new);
		sequenceDeltaRegistry.register(8, GSChannelEntryTypeDelta.class, GSChannelEntryTypeDelta::new);
		sequenceDeltaRegistry.register(9, GSChannelMovedDelta.class, GSChannelMovedDelta::new);
		
		compositionDeltaRegistry = new GSSupplierRegistry<>();
		
		compositionDeltaRegistry.register( 0, GSCompositionNameDelta.class, GSCompositionNameDelta::new);
		compositionDeltaRegistry.register( 1, GSGroupAddedDelta.class, GSGroupAddedDelta::new);
		compositionDeltaRegistry.register( 2, GSGroupRemovedDelta.class, GSGroupRemovedDelta::new);
		compositionDeltaRegistry.register( 3, GSGroupNameDelta.class, GSGroupNameDelta::new);
		compositionDeltaRegistry.register( 4, GSTrackAddedDelta.class, GSTrackAddedDelta::new);
		compositionDeltaRegistry.register( 5, GSTrackRemovedDelta.class, GSTrackRemovedDelta::new);
		compositionDeltaRegistry.register( 6, GSTrackNameDelta.class, GSTrackNameDelta::new);
		compositionDeltaRegistry.register( 7, GSTrackColorDelta.class, GSTrackColorDelta::new);
		compositionDeltaRegistry.register( 8, GSTrackGroupDelta.class, GSTrackGroupDelta::new);
		compositionDeltaRegistry.register( 9, GSTrackSequenceDelta.class, GSTrackSequenceDelta::new);
		compositionDeltaRegistry.register(10, GSTrackEntryAddedDelta.class, GSTrackEntryAddedDelta::new);
		compositionDeltaRegistry.register(11, GSTrackEntryRemovedDelta.class, GSTrackEntryRemovedDelta::new);
		compositionDeltaRegistry.register(12, GSTrackEntryOffsetDelta.class, GSTrackEntryOffsetDelta::new);
		
		signalEventHandlerRegistry = new IdentityHashMap<>();
		
		signalEventHandlerRegistry.put(Blocks.PISTON, new GSPistonSignalEventHandler());
		signalEventHandlerRegistry.put(Blocks.STICKY_PISTON, new GSPistonSignalEventHandler());
		signalEventHandlerRegistry.put(Blocks.NOTE_BLOCK, new GSNoteBlockSignalEventHandler());
	}
	
	@Override
	public void registerPackets(GSSupplierRegistry<Integer, GSIPacket> registry) {
		registry.register(1, GSStartCompositionSessionPacket.class, GSStartCompositionSessionPacket::new);
		registry.register(2, GSStopCompositionSessionPacket.class, GSStopCompositionSessionPacket::new);
		registry.register(3, GSStartSequenceSessionPacket.class, GSStartSequenceSessionPacket::new);
		registry.register(4, GSStopSequenceSessionPacket.class, GSStopSequenceSessionPacket::new);
		registry.register(5, GSCompositionDeltaPacket.class, GSCompositionDeltaPacket::new);
		registry.register(6, GSResetCompositionPacket.class, GSResetCompositionPacket::new);
		registry.register(7, GSCompositionSessionChangedPacket.class, GSCompositionSessionChangedPacket::new);
		registry.register(8, GSSequenceSessionChangedPacket.class, GSSequenceSessionChangedPacket::new);
		registry.register(9, GSSequenceSessionRequestPacket.class, GSSequenceSessionRequestPacket::new);
	}
	
	@Override
	public void addClientModules(GSClientController controller) {
		controller.addModule(getClientModule());
	}

	@Override
	public void addServerModules(GSServerController controller) {
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
	public GSCapturePlaybackClientModule getClientModule() {
		if (clientModule == null)
			clientModule = new GSCapturePlaybackClientModule();
		return clientModule;
	}

	public GSCapturePlaybackServerModule getServerModule() {
		if (serverModule == null)
			serverModule = new GSCapturePlaybackServerModule();
		return serverModule;
	}
	
	public GSSupplierRegistry<Integer, GSISequenceDelta> getSequenceDeltaRegistry() {
		return sequenceDeltaRegistry;
	}

	public GSSupplierRegistry<Integer, GSICompositionDelta> getCompositionDeltaRegistry() {
		return compositionDeltaRegistry;
	}
	
	public Map<Block, GSISignalEventHandler> getSignalEventHandlerRegistry() {
		return signalEventHandlerRegistry;
	}
}
