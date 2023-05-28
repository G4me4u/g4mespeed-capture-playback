package com.g4mesoft.captureplayback;

import java.util.IdentityHashMap;
import java.util.Map;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.GSExtensionUID;
import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.common.asset.GSAssetCollaboratorPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetHistoryPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoChangedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfoRemovedPacket;
import com.g4mesoft.captureplayback.common.asset.GSAssetRequestResponsePacket;
import com.g4mesoft.captureplayback.common.asset.GSCreateAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSDeleteAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSImportAssetPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCacheEntryAddedPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCacheEntryRemovedPacket;
import com.g4mesoft.captureplayback.common.asset.GSPlayerCachePacket;
import com.g4mesoft.captureplayback.common.asset.GSRequestAssetPacket;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionRequestPacket;
import com.g4mesoft.captureplayback.session.GSSessionStartPacket;
import com.g4mesoft.captureplayback.session.GSSessionStopPacket;
import com.g4mesoft.captureplayback.stream.handler.GSGeneralBlockSignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSISignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSPistonSignalEventHandler;
import com.g4mesoft.captureplayback.stream.handler.GSTntSignalEventHandler;
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
	public static final GSVersion VERSION = new GSVersion(0, 5, 1);
	
	public static final GSExtensionInfo INFO = new GSExtensionInfo(NAME, UID, VERSION);
	
	private static final String TRANSLATION_PATH = "/assets/g4mespeed-capture-playback/lang/en.lang";
	
	private static GSCapturePlaybackExtension instance;
	
	private Map<Block, GSISignalEventHandler> signalEventHandlerRegistry;
	
	@Environment(EnvType.CLIENT)
	private GSCapturePlaybackClientModule clientModule;
	private GSCapturePlaybackServerModule serverModule;
	
	@Override
	public void init() {
		instance = this;
		
		signalEventHandlerRegistry = new IdentityHashMap<>();
		
		GSISignalEventHandler pistonHandler = new GSPistonSignalEventHandler();
		addSignalEventHandler(Blocks.PISTON, pistonHandler);
		addSignalEventHandler(Blocks.STICKY_PISTON, pistonHandler);
		addSignalEventHandler(Blocks.TNT, new GSTntSignalEventHandler());
		GSISignalEventHandler generalBlockHandler = new GSGeneralBlockSignalEventHandler();
		addSignalEventHandler(Blocks.NOTE_BLOCK, generalBlockHandler);
		addSignalEventHandler(Blocks.REDSTONE_WIRE, generalBlockHandler);
		addSignalEventHandler(Blocks.POWERED_RAIL, generalBlockHandler);
		addSignalEventHandler(Blocks.ACTIVATOR_RAIL, generalBlockHandler);
		addSignalEventHandler(Blocks.BELL, generalBlockHandler);
		// Add doors
		addSignalEventHandler(Blocks.IRON_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.OAK_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.SPRUCE_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.BIRCH_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.JUNGLE_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.ACACIA_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.DARK_OAK_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.CRIMSON_DOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.WARPED_DOOR, generalBlockHandler);
		// Add trap-doors
		addSignalEventHandler(Blocks.IRON_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.OAK_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.SPRUCE_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.BIRCH_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.JUNGLE_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.ACACIA_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.DARK_OAK_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.CRIMSON_TRAPDOOR, generalBlockHandler);
		addSignalEventHandler(Blocks.WARPED_TRAPDOOR, generalBlockHandler);
		// Add fence gates
		addSignalEventHandler(Blocks.OAK_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.SPRUCE_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.BIRCH_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.JUNGLE_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.ACACIA_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.DARK_OAK_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.CRIMSON_FENCE_GATE, generalBlockHandler);
		addSignalEventHandler(Blocks.WARPED_FENCE_GATE, generalBlockHandler);
	}
	
	private void addSignalEventHandler(Block block, GSISignalEventHandler handler) {
		signalEventHandlerRegistry.put(block, handler);
	}
	
	@Override
	public void registerPackets(GSSupplierRegistry<Integer, GSIPacket> registry) {
		registry.register(10, GSSessionRequestPacket.class, GSSessionRequestPacket::new);
		registry.register(11, GSSessionStartPacket.class, GSSessionStartPacket::new);
		registry.register(12, GSSessionStopPacket.class, GSSessionStopPacket::new);
		registry.register(13, GSSessionDeltasPacket.class, GSSessionDeltasPacket::new);

		registry.register(14, GSAssetHistoryPacket.class, GSAssetHistoryPacket::new);
		registry.register(15, GSAssetInfoChangedPacket.class, GSAssetInfoChangedPacket::new);
		registry.register(16, GSAssetInfoRemovedPacket.class, GSAssetInfoRemovedPacket::new);
		registry.register(17, GSCreateAssetPacket.class, GSCreateAssetPacket::new);
		registry.register(18, GSDeleteAssetPacket.class, GSDeleteAssetPacket::new);
		registry.register(19, GSImportAssetPacket.class, GSImportAssetPacket::new);
		registry.register(20, GSRequestAssetPacket.class, GSRequestAssetPacket::new);
		registry.register(21, GSAssetRequestResponsePacket.class, GSAssetRequestResponsePacket::new);
		
		registry.register(22, GSPlayerCachePacket.class, GSPlayerCachePacket::new);
		registry.register(23, GSPlayerCacheEntryAddedPacket.class, GSPlayerCacheEntryAddedPacket::new);
		registry.register(24, GSPlayerCacheEntryRemovedPacket.class, GSPlayerCacheEntryRemovedPacket::new);

		registry.register(25, GSAssetCollaboratorPacket.class, GSAssetCollaboratorPacket::new);
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
	
	public GSISignalEventHandler getSignalEventHandler(Block block) {
		return signalEventHandlerRegistry.get(block);
	}

	public boolean hasSignalEventHandler(Block block) {
		return signalEventHandlerRegistry.containsKey(block);
	}
	
	public static GSCapturePlaybackExtension getInstance() {
		return instance;
	}
}
