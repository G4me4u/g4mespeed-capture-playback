package com.g4mesoft.captureplayback.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.gui.GSCapturePlaybackGUI;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.delta.GSITimelineDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSITimelineDeltaListener;
import com.g4mesoft.captureplayback.timeline.delta.GSTimelineDeltaException;
import com.g4mesoft.captureplayback.timeline.delta.GSTimelineDeltaTransformer;
import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.client.GSIModuleManagerClient;
import com.g4mesoft.core.server.GSIModuleManagerServer;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSKeyBinding;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.util.GSFileUtils;
import com.mojang.brigadier.CommandDispatcher;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;

public class GSCapturePlaybackModule implements GSIModule, GSITimelineDeltaListener {

	private static final String GUI_TAB_TITLE = "gui.tab.capture-playback";
	
	public static final String KEY_CATEGORY = "capture-playback";
	
	public static final String TIMELINE_FILE_NAME = "active_timeline.dat";
	
	private final GSTimeline activeTimeline;
	private final GSTimelineDeltaTransformer transformer;

	private GSIModuleManager manager;
	
	@Environment(EnvType.CLIENT)
	private GSKeyBinding collapseTabKey;
	@Environment(EnvType.CLIENT)
	private GSKeyBinding expandedHoveredTabKey;
	
	public GSCapturePlaybackModule() {
		activeTimeline = new GSTimeline();
		
		transformer = new GSTimelineDeltaTransformer();
		transformer.addDeltaListener(this);
	}
	
	@Override
	public void init(GSIModuleManager manager) {
		this.manager = manager;

		transformer.install(activeTimeline);

		manager.runOnServer((serverManager) -> {
			try {
				activeTimeline.set(readTimeline(getTimelineFile()));
			} catch (IOException e) {
				CapturePlaybackMod.GSP_LOGGER.warn("Unable to read active timeline!");
			}
		});
	}
	
	@Override
	public void onClose() {
		manager.runOnServer((serverManager) -> {
			try {
				writeTimeline(activeTimeline, getTimelineFile());
			} catch (IOException e) {
				CapturePlaybackMod.GSP_LOGGER.warn("Unable to write active timeline!");
			}
		});
		
		manager = null;
		
		transformer.uninstall(activeTimeline);
	}

	private GSTimeline readTimeline(File timelineFile) throws IOException {
		GSTimeline timeline;
		
		try (FileInputStream fis = new FileInputStream(timelineFile)) {
			byte[] data = IOUtils.toByteArray(fis);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(data));
			timeline = GSTimeline.read(buffer);
			buffer.release();
		}
		
		return timeline;
	}
	
	private void writeTimeline(GSTimeline timeline, File timelineFile) throws IOException {
		GSFileUtils.ensureFileExists(timelineFile);
		
		try (FileOutputStream fos = new FileOutputStream(timelineFile)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			GSTimeline.write(buffer, timeline);
			if (buffer.hasArray()) {
				fos.write(buffer.array(), buffer.arrayOffset(), buffer.writerIndex());
			} else {
				fos.getChannel().write(buffer.nioBuffer());
			}
			buffer.release();
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initGUI(GSTabbedGUI tabbedGUI) {
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSCapturePlaybackGUI(this));
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void registerHotkeys(GSKeyManager keyManager) {
		collapseTabKey = keyManager.registerKey("collapseTab", KEY_CATEGORY, GLFW.GLFW_KEY_T);
		expandedHoveredTabKey = keyManager.registerKey("expandedHoveredTab", KEY_CATEGORY, GLFW.GLFW_KEY_T);
	}
	
	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		GSPlaybackCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSVersion version) {
		manager.runOnServer(ms -> sendPacket(ms, new GSTimelinePacket(activeTimeline), player));
	}
	
	public void onTimelineReceived(GSTimeline timeline) {
		manager.runOnClient(managerClient -> {
			transformer.setEnabled(false);
			activeTimeline.set(timeline);
			transformer.setEnabled(true);
		});
	}
	
	@Override
	public void onTimelineDelta(GSITimelineDelta delta) {
		manager.runOnClient(managerClient -> {
			sendPacket(managerClient, new GSTimelineDeltaPacket(delta));
		});
	}

	public void onClientDeltaReceived(GSITimelineDelta delta, ServerPlayerEntity player) {
		manager.runOnServer(managerServer -> {
			transformer.setEnabled(false);
			try {
				delta.applyDelta(activeTimeline);
				
				sendPacketToAllExcept(managerServer, new GSTimelineDeltaPacket(delta), player);
			} catch (GSTimelineDeltaException e) {
				managerServer.sendPacket(new GSTimelinePacket(activeTimeline), player);	
			}
			transformer.setEnabled(true);
		});
	}

	@Environment(EnvType.CLIENT)
	public void onServerDeltaReceived(GSITimelineDelta delta) {
		manager.runOnClient(managerClient -> {
			transformer.setEnabled(false);
			try {
				delta.applyDelta(activeTimeline);
			} catch (GSTimelineDeltaException ignore) {
			}
			transformer.setEnabled(true);
		});
	}

	private void sendPacket(GSIModuleManagerClient managerClient, GSIPacket packet) {
		if (managerClient.isServerExtensionInstalled(CapturePlaybackMod.EXTENSION_UID))
			managerClient.sendPacket(packet);
	}

	private void sendPacket(GSIModuleManagerServer managerServer, GSIPacket packet, ServerPlayerEntity player) {
		if (managerServer.isExtensionInstalled(player, CapturePlaybackMod.EXTENSION_UID))
			managerServer.sendPacket(packet, player);
	}

	private void sendPacketToAllExcept(GSIModuleManagerServer managerServer, GSIPacket packet, ServerPlayerEntity player) {
		for (ServerPlayerEntity otherPlayer : managerServer.getAllPlayers()) {
			if (otherPlayer != player)
				sendPacket(managerServer, packet, otherPlayer);
		}
	}
	
	private File getTimelineFile() {
		return new File(manager.getCacheFile(), TIMELINE_FILE_NAME);
	}
	
	@Environment(EnvType.CLIENT)
	public GSKeyBinding getCollapseTabKey() {
		return collapseTabKey;
	}

	@Environment(EnvType.CLIENT)
	public GSKeyBinding getExpandHoveredTabKey() {
		return expandedHoveredTabKey;
	}
	
	public GSTimeline getActiveTimeline() {
		return activeTimeline;
	}
}
