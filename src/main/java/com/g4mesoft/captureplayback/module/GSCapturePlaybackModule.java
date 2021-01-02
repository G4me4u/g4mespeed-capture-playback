package com.g4mesoft.captureplayback.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaException;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;
import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSKeyBinding;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.util.GSFileUtils;
import com.mojang.brigadier.CommandDispatcher;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;

public class GSCapturePlaybackModule implements GSIModule, GSISequenceDeltaListener {

	private static final String GUI_TAB_TITLE = "gui.tab.capture-playback";
	
	public static final String KEY_CATEGORY = "capture-playback";
	
	public static final String SEQUENCE_FILE_NAME = "active_sequence.gsq";
	
	private final GSSequence activeSequence;
	private final GSSequenceDeltaTransformer transformer;

	private GSIModuleManager manager;
	
	@Environment(EnvType.CLIENT)
	private GSKeyBinding collapseTabKey;
	@Environment(EnvType.CLIENT)
	private GSKeyBinding expandedHoveredTabKey;
	
	public GSCapturePlaybackModule() {
		activeSequence = new GSSequence(UUID.randomUUID());
		
		transformer = new GSSequenceDeltaTransformer();
		transformer.addDeltaListener(this);
	}
	
	@Override
	public void init(GSIModuleManager manager) {
		this.manager = manager;

		transformer.install(activeSequence);

		manager.runOnServer((serverManager) -> {
			try {
				activeSequence.set(readSequence(getSequenceFile()));
			} catch (IOException e) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to read active sequence!");
			}
		});
		
		manager.runOnClient((clientManager) -> {
			clientManager.addRenderable(new GSSequencePositionRenderable(activeSequence));
		});
	}
	
	@Override
	public void onClose() {
		manager.runOnServer((serverManager) -> {
			try {
				writeSequence(activeSequence, getSequenceFile());
			} catch (IOException e) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to write active sequence!");
			}
		});
		
		manager = null;
		
		transformer.uninstall(activeSequence);
	}

	private GSSequence readSequence(File sequenceFile) throws IOException {
		GSSequence sequence;
		
		try (FileInputStream fis = new FileInputStream(sequenceFile)) {
			byte[] data = IOUtils.toByteArray(fis);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(data));
			sequence = GSSequence.read(buffer);
			buffer.release();
		}
		
		return sequence;
	}
	
	private void writeSequence(GSSequence sequence, File sequenceFile) throws IOException {
		GSFileUtils.ensureFileExists(sequenceFile);
		
		try (FileOutputStream fos = new FileOutputStream(sequenceFile)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			GSSequence.write(buffer, sequence);
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
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSCapturePlaybackPanel(this));
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
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSExtensionInfo coreInfo) {
		manager.runOnServer(managerServer -> {
			managerServer.sendPacket(new GSSequencePacket(activeSequence), player);	
		});
	}
	
	public void onSequenceReceived(GSSequence sequence) {
		manager.runOnClient(managerClient -> {
			try {
				transformer.setEnabled(false);
				activeSequence.set(sequence);
			} finally {
				transformer.setEnabled(true);
			}
		});
	}
	
	@Override
	public void onSequenceDelta(GSISequenceDelta delta) {
		manager.runOnClient(managerClient -> {
			managerClient.sendPacket(new GSSequenceDeltaPacket(delta));
		});
	}

	public void onClientDeltaReceived(GSISequenceDelta delta, ServerPlayerEntity player) {
		manager.runOnServer(managerServer -> {
			try {
				transformer.setEnabled(false);
				delta.applyDelta(activeSequence);
				
				managerServer.sendPacketToAllExcept(new GSSequenceDeltaPacket(delta), player);
			} catch (GSSequenceDeltaException ignore) {
				// The delta could not be applied. Probably because of a desync, or
				// because multiple users are changing the same part of the sequence.
				managerServer.sendPacket(new GSSequencePacket(activeSequence), player);	
			} finally {
				transformer.setEnabled(true);
			}
		});
	}

	@Environment(EnvType.CLIENT)
	public void onServerDeltaReceived(GSISequenceDelta delta) {
		manager.runOnClient(managerClient -> {
			try {
				transformer.setEnabled(false);
				delta.applyDelta(activeSequence);
			} catch (GSSequenceDeltaException ignore) {
			} finally {
				transformer.setEnabled(true);
			}
		});
	}

	private File getSequenceFile() {
		return new File(manager.getCacheFile(), SEQUENCE_FILE_NAME);
	}
	
	@Environment(EnvType.CLIENT)
	public GSKeyBinding getCollapseTabKey() {
		return collapseTabKey;
	}

	@Environment(EnvType.CLIENT)
	public GSKeyBinding getExpandHoveredTabKey() {
		return expandedHoveredTabKey;
	}
	
	public GSSequence getActiveSequence() {
		return activeSequence;
	}
}
