package com.g4mesoft.captureplayback;

import java.util.IdentityHashMap;
import java.util.Map;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.GSExtensionUID;
import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.module.server.GSCapturePlaybackServerModule;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionRequestPacket;
import com.g4mesoft.captureplayback.session.GSSessionStartPacket;
import com.g4mesoft.captureplayback.session.GSSessionStopPacket;
import com.g4mesoft.captureplayback.stream.handler.GSBlockPowerEventHandler;
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
	public static final GSVersion VERSION = new GSVersion(0, 2, 1);
	
	public static final GSExtensionInfo INFO = new GSExtensionInfo(NAME, UID, VERSION);
	
	private static final String TRANSLATION_PATH = "/assets/g4mespeed/captureplayback/lang/en.lang";
	
	private Map<Block, GSISignalEventHandler> signalEventHandlerRegistry;
	
	@Environment(EnvType.CLIENT)
	private GSCapturePlaybackClientModule clientModule;
	private GSCapturePlaybackServerModule serverModule;
	
	@Override
	public void init() {
		signalEventHandlerRegistry = new IdentityHashMap<>();
		
		GSISignalEventHandler pistonHandler = new GSPistonSignalEventHandler();
		addSignalEventHandler(Blocks.PISTON, pistonHandler);
		addSignalEventHandler(Blocks.STICKY_PISTON, pistonHandler);
		addSignalEventHandler(Blocks.NOTE_BLOCK, new GSNoteBlockSignalEventHandler());
		GSISignalEventHandler blockPowerHandler = new GSBlockPowerEventHandler();
		addSignalEventHandler(Blocks.TNT, blockPowerHandler);
		addSignalEventHandler(Blocks.REDSTONE_WIRE, blockPowerHandler);
		addSignalEventHandler(Blocks.POWERED_RAIL, blockPowerHandler);
		addSignalEventHandler(Blocks.ACTIVATOR_RAIL, blockPowerHandler);
		// Add doors
		addSignalEventHandler(Blocks.IRON_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.OAK_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.SPRUCE_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.BIRCH_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.JUNGLE_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.ACACIA_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.DARK_OAK_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.CRIMSON_DOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.WARPED_DOOR, blockPowerHandler);
		// Add trap-doors
		addSignalEventHandler(Blocks.IRON_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.OAK_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.SPRUCE_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.BIRCH_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.JUNGLE_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.ACACIA_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.DARK_OAK_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.CRIMSON_TRAPDOOR, blockPowerHandler);
		addSignalEventHandler(Blocks.WARPED_TRAPDOOR, blockPowerHandler);
		// Add fence gates
		addSignalEventHandler(Blocks.OAK_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.SPRUCE_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.BIRCH_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.JUNGLE_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.ACACIA_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.DARK_OAK_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.CRIMSON_FENCE_GATE, blockPowerHandler);
		addSignalEventHandler(Blocks.WARPED_FENCE_GATE, blockPowerHandler);
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
	
}
