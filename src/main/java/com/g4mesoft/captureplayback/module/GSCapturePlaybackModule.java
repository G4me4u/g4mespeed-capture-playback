package com.g4mesoft.captureplayback.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.gui.GSDefaultChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaException;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;
import com.g4mesoft.core.GSIModule;
import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSEKeyEventType;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.setting.GSSettingCategory;
import com.g4mesoft.setting.GSSettingManager;
import com.g4mesoft.setting.types.GSIntegerSetting;
import com.g4mesoft.util.GSFileUtil;
import com.mojang.brigadier.CommandDispatcher;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class GSCapturePlaybackModule implements GSIModule, GSISequenceDeltaListener {

	public static final String ACTIVE_SEQUENCE_FILE_NAME = "active_sequence";
	public static final String SEQUENCE_EXTENSION = ".gsq";

	private static final String GUI_TAB_TITLE = "gui.tab.capture-playback";
	public static final String KEY_CATEGORY = "capture-playback";
	public static final GSSettingCategory CAPTURE_PLAYBACK_CATEGORY = new GSSettingCategory("capture-playback");
	
	public static final int RENDERING_DISABLED = 0;
	public static final int RENDERING_DEPTH    = 1;
	public static final int RENDERING_NO_DEPTH = 2;
	
	private final GSSequenceSession sequenceSession;
	private final GSSequenceDeltaTransformer transformer;

	private GSIModuleManager manager;

	public final GSIntegerSetting cChannelRenderingType;
	
	public GSCapturePlaybackModule() {
		sequenceSession = new GSSequenceSession(new GSSequence(UUID.randomUUID()));
		
		transformer = new GSSequenceDeltaTransformer();
		transformer.addDeltaListener(this);
		
		manager = null;
		
		cChannelRenderingType = new GSIntegerSetting("channelRenderingType", RENDERING_DISABLED, 0, 2);
	}
	
	@Override
	public void init(GSIModuleManager manager) {
		this.manager = manager;

		transformer.install(getActiveSequence());

		manager.runOnServer((serverManager) -> {
			try {
				setActiveSequence(readSequence(getSequenceFile(ACTIVE_SEQUENCE_FILE_NAME)));
			} catch (IOException e) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to read active sequence!");
			}
		});
		
		manager.runOnClient((clientManager) -> {
			clientManager.addRenderable(new GSSequencePositionRenderable(this, getActiveSequence()));
		});
	}
	
	@Override
	public void onClose() {
		manager.runOnServer((serverManager) -> {
			try {
				writeSequence(getActiveSequence(), getSequenceFile(ACTIVE_SEQUENCE_FILE_NAME));
			} catch (IOException e) {
				CapturePlaybackMod.GSCP_LOGGER.warn("Unable to write active sequence!");
			}
		});
		
		manager = null;
		
		transformer.uninstall(getActiveSequence());
	}

	public GSSequence readSequence(String fileName) {
		GSSequence sequence = null;
		try {
			sequence = readSequence(getSequenceFile(fileName));
		} catch (IOException e) {
		}
		return sequence;
	}
	
	private GSSequence readSequence(File sequenceFile) throws IOException {
		GSSequence sequence;
		
		try (FileInputStream fis = new FileInputStream(sequenceFile)) {
			byte[] data = IOUtils.toByteArray(fis);
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(data));
			sequence = GSSequence.read(buffer);
			buffer.release();
		} catch (Throwable throwable) {
			throw new IOException("Unable to read sequence", throwable);
		}
		
		return sequence;
	}
	
	public boolean writeSequence(GSSequence sequence, String fileName) {
		try {
			writeSequence(sequence, getSequenceFile(fileName));
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	private void writeSequence(GSSequence sequence, File sequenceFile) throws IOException {
		GSFileUtil.ensureFileExists(sequenceFile);
		
		try (FileOutputStream fos = new FileOutputStream(sequenceFile)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			GSSequence.write(buffer, sequence);
			if (buffer.hasArray()) {
				fos.write(buffer.array(), buffer.arrayOffset(), buffer.writerIndex());
			} else {
				fos.getChannel().write(buffer.nioBuffer());
			}
			buffer.release();
		} catch (Throwable throwable) {
			throw new IOException("Unable to write sequence", throwable);
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
		keyManager.registerKey("channelRenderingType", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			int newValue = cChannelRenderingType.getValue() + 1;
			if (newValue > cChannelRenderingType.getMaxValue())
				newValue = cChannelRenderingType.getMinValue();
			cChannelRenderingType.setValue(newValue);
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("newChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			BlockPos position = getCrosshairTarget();
			// Only add channels if we have a crosshair target
			if (position != null) {
				GSSequence activeSequence = getActiveSequence();
				String name = GSDefaultChannelProvider.getDefaultChannelName();
				int color = GSDefaultChannelProvider.getUniqueColor(activeSequence);
				GSChannel channel = activeSequence.addChannel(new GSChannelInfo(name, color, position));
				
				// Automatically select the new channel
				if (channel != null)
					sequenceSession.setSelectedChannelUUID(channel.getChannelUUID());
			}
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("extendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequenceSession.getSelectedChannel();
			if (position != null && channel != null)
				channel.setInfo(channel.getInfo().addPosition(position));
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("unextendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequenceSession.getSelectedChannel();
			if (position != null && channel != null) {
				GSChannelInfo info = channel.getInfo();
				// Ensure that we have at least one position in the channel
				if (info.getPositions().size() > 1)
					channel.setInfo(info.removePosition(position));
			}
		}, GSEKeyEventType.PRESS);

		keyManager.registerKey("selectChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			GSChannel channel = getCrosshairChannel();
			if (channel != null)
				sequenceSession.setSelectedChannelUUID(channel.getChannelUUID());
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("pasteChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			GSChannel selectedChannel = sequenceSession.getSelectedChannel();
			if (selectedChannel != null)
				return channel.getInfo().withColor(selectedChannel.getInfo().getColor());
			return channel.getInfo();
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("brightenChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSIRenderer.brightenColor(channel.getInfo().getColor()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("darkenChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSIRenderer.darkenColor(channel.getInfo().getColor()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("randomizeChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSDefaultChannelProvider.getUniqueColor(channel.getParent()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);
	}
	
	public static BlockPos getCrosshairTarget() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget == null)
			return null;
		if (client.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return null;
		return ((BlockHitResult)client.crosshairTarget).getBlockPos();
	}
	
	private GSChannel getCrosshairChannel() {
		BlockPos position = getCrosshairTarget();
		if (position != null) {
			for (GSChannel channel : getActiveSequence().getChannels()) {
				if (channel.getInfo().getPositions().contains(position))
					return channel;
			}
		}
		
		return null;
	}
	
	private void modifyCrosshairChannel(Function<GSChannel, GSChannelInfo> modifier) {
		GSChannel channel = getCrosshairChannel();
		if (channel != null)
			channel.setInfo(modifier.apply(channel));
	}
	
	@Override
	public void registerClientSettings(GSSettingManager settings) {
		settings.registerSetting(CAPTURE_PLAYBACK_CATEGORY, cChannelRenderingType);
	}
	
	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		GSPlaybackCommand.registerCommand(dispatcher);
		GSCaptureCommand.registerCommand(dispatcher);
		GSSequenceCommand.registerCommand(dispatcher);
	}
	
	@Override
	public void onG4mespeedClientJoin(ServerPlayerEntity player, GSExtensionInfo coreInfo) {
		manager.runOnServer(managerServer -> {
			managerServer.sendPacket(new GSSequencePacket(getActiveSequence()), player);	
		});
	}
	
	public void onSequenceReceived(GSSequence sequence) {
		manager.runOnClient(managerClient -> {
			try {
				disableDeltaTransformer();
				getActiveSequence().set(sequence);
			} finally {
				enableDeltaTransformer();
			}
		});
	}
	
	@Override
	public void onSequenceDelta(GSISequenceDelta delta) {
		manager.runOnClient(managerClient -> {
			managerClient.sendPacket(new GSSequenceDeltaPacket(delta));
		});
		manager.runOnServer(managerServer -> {
			managerServer.sendPacketToAll(new GSSequenceDeltaPacket(delta));
		});
	}

	public void onClientDeltaReceived(GSISequenceDelta delta, ServerPlayerEntity player) {
		manager.runOnServer(managerServer -> {
			try {
				disableDeltaTransformer();
				delta.applyDelta(getActiveSequence());
				
				managerServer.sendPacketToAllExcept(new GSSequenceDeltaPacket(delta), player);
			} catch (GSSequenceDeltaException ignore) {
				// The delta could not be applied. Probably because of a de-sync, or
				// because multiple users are changing the same part of the sequence.
				managerServer.sendPacket(new GSSequencePacket(getActiveSequence()), player);
			} finally {
				enableDeltaTransformer();
			}
		});
	}

	@Environment(EnvType.CLIENT)
	public void onServerDeltaReceived(GSISequenceDelta delta) {
		manager.runOnClient(managerClient -> {
			try {
				disableDeltaTransformer();
				delta.applyDelta(getActiveSequence());
			} catch (GSSequenceDeltaException ignore) {
			} finally {
				enableDeltaTransformer();
			}
		});
	}
	
	private File getSequenceFile(String fileName) {
		return new File(getSequenceDirectory(), fileName + SEQUENCE_EXTENSION);
	}

	public File getSequenceDirectory() {
		return manager.getCacheFile();
	}
	
	public GSSequenceSession getSequenceSession() {
		return sequenceSession;
	}
	
	public GSSequence getActiveSequence() {
		return sequenceSession.getActiveSequence();
	}
	
	public void setActiveSequence(GSSequence sequence) {
		try {
			disableDeltaTransformer();
			getActiveSequence().set(sequence);
		} finally {
			enableDeltaTransformer();
		}
		
		manager.runOnServer((managerServer) -> {
			managerServer.sendPacketToAll(new GSSequencePacket(getActiveSequence()));
		});
	}
	
	private void disableDeltaTransformer() {
		transformer.setEnabled(false);
		sequenceSession.getUndoRedoHistory().stopTracking();
	}

	private void enableDeltaTransformer() {
		transformer.setEnabled(true);
		sequenceSession.getUndoRedoHistory().startTracking();
	}
}
