package com.g4mesoft.captureplayback.module.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaException;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaTransformer;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDeltaListener;
import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.gui.GSDefaultChannelProvider;
import com.g4mesoft.captureplayback.gui.GSSequenceEditPanel;
import com.g4mesoft.captureplayback.module.GSCompositionDeltaPacket;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.GSCompositionSessionChangedPacket;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.captureplayback.module.GSSequenceSessionChangedPacket;
import com.g4mesoft.captureplayback.module.GSSequenceSessionRequestPacket;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.client.GSIClientModule;
import com.g4mesoft.core.client.GSIClientModuleManager;
import com.g4mesoft.gui.GSContentHistoryGUI;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSEKeyEventType;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.setting.GSSettingCategory;
import com.g4mesoft.setting.GSSettingManager;
import com.g4mesoft.setting.types.GSIntegerSetting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class GSCapturePlaybackClientModule implements GSIClientModule, GSICompositionDeltaListener, GSISequenceDeltaListener {

	public static final int RENDERING_DISABLED = 0;
	public static final int RENDERING_DEPTH    = 1;
	public static final int RENDERING_NO_DEPTH = 2;
	
	private static final String GUI_TAB_TITLE = "gui.tab.capture-playback";
	public static final String KEY_CATEGORY = "capture-playback";
	public static final GSSettingCategory CAPTURE_PLAYBACK_CATEGORY = new GSSettingCategory("capture-playback");
	
	private final GSCompositionDeltaTransformer compositionTransformer;
	private final GSSequenceDeltaTransformer sequenceTransformer;

	private GSCompositionSession compositionSession;
	private GSSequenceSession sequenceSession;
	
	private final Map<UUID, GSComposition> activeCompositions;
	private final Map<UUID, GSSequence> activeSequences;
	
	private GSIClientModuleManager manager;
	
	public final GSIntegerSetting cChannelRenderingType;
	
	public GSCapturePlaybackClientModule() {
		compositionTransformer = new GSCompositionDeltaTransformer();
		compositionTransformer.addDeltaListener(this);
		sequenceTransformer = new GSSequenceDeltaTransformer();
		sequenceTransformer.addDeltaListener(this);

		compositionSession = null;
		sequenceSession = null;
		
		activeCompositions = new HashMap<>();
		activeSequences = new HashMap<>();
		
		manager = null;
		
		cChannelRenderingType = new GSIntegerSetting("channelRenderingType", RENDERING_DISABLED, 0, 2);
	}
	
	@Override
	public void init(GSIClientModuleManager manager) {
		this.manager = manager;
		
		manager.addRenderable(new GSSequencePositionRenderable(this));
	}
	
	@Override
	public void onClose() {
		manager = null;
	}
	
	@Override
	public void initGUI(GSTabbedGUI tabbedGUI) {
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSCapturePlaybackPanel(this));
	}
	
	@Override
	public void registerHotkeys(GSKeyManager keyManager) {
		keyManager.registerKey("channelRenderingType", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			int newValue = cChannelRenderingType.getValue() + 1;
			if (newValue > cChannelRenderingType.getMaxValue())
				newValue = cChannelRenderingType.getMinValue();
			cChannelRenderingType.setValue(newValue);
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("newChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::createNewChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("extendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::extendChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("unextendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::unextendChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("selectChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			GSChannel channel = getSessionChannelAtCrosshair();
			if (channel != null)
				sequenceSession.setSelectedChannelUUID(channel.getChannelUUID());
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("pasteChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			GSSequence sequence = getSessionSequence();
			
			if (sequence != null && sequenceSession != null) {
				GSChannel selectedChannel = sequence.getChannel(sequenceSession.getSelectedChannelUUID());
				if (selectedChannel != null)
					return channel.getInfo().withColor(selectedChannel.getInfo().getColor());
			}
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
	
	private void createNewChannel() {
		GSSequence sequence = getSessionSequence();
		
		if (sequence != null) {
			BlockPos position = getCrosshairTarget();
			// Only add channels if we have a crosshair target
			if (position != null) {
				String name = GSDefaultChannelProvider.getDefaultChannelName();
				int color = GSDefaultChannelProvider.getUniqueColor(sequence);
				GSChannel channel = sequence.addChannel(new GSChannelInfo(name, color, position));
				
				// Automatically select the new channel
				if (channel != null && sequenceSession != null)
					sequenceSession.setSelectedChannelUUID(channel.getChannelUUID());
			}
		}
	}
	
	private void extendChannel() {
		GSSequence sequence = getSessionSequence();
		
		if (sequence != null && sequenceSession != null) {
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequence.getChannel(sequenceSession.getSelectedChannelUUID());
			
			if (position != null && channel != null)
				channel.setInfo(channel.getInfo().addPosition(position));
		}
	}

	private void unextendChannel() {
		GSSequence sequence = getSessionSequence();
		
		if (sequence != null && sequenceSession != null) {
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequence.getChannel(sequenceSession.getSelectedChannelUUID());
			
			if (position != null && channel != null) {
				GSChannelInfo info = channel.getInfo();
				// Ensure that we have at least one position in the channel
				if (info.getPositions().size() > 1)
					channel.setInfo(info.removePosition(position));
			}
		}
	}
	
	private GSChannel getSessionChannelAtCrosshair() {
		GSSequence sequence = getSessionSequence();
		
		if (sequence != null) {
			BlockPos position = getCrosshairTarget();

			if (position != null) {
				for (GSChannel channel : sequence.getChannels()) {
					if (channel.getInfo().getPositions().contains(position))
						return channel;
				}
			}
		}
		
		return null;
	}
	
	private void modifyCrosshairChannel(Function<GSChannel, GSChannelInfo> modifier) {
		GSChannel channel = getSessionChannelAtCrosshair();
		if (channel != null)
			channel.setInfo(modifier.apply(channel));
	}

	public static BlockPos getCrosshairTarget() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget == null)
			return null;
		if (client.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return null;
		return ((BlockHitResult)client.crosshairTarget).getBlockPos();
	}
	
	@Override
	public void registerClientSettings(GSSettingManager settings) {
		settings.registerSetting(CAPTURE_PLAYBACK_CATEGORY, cChannelRenderingType);
	}
	
	@Override
	public void onDisconnectServer() {
		onStopSequenceSession();
		onStopCompositionSession();
	}
	
	public void onStartCompositionSession(GSCompositionSession session, GSComposition composition) {
		this.compositionSession = session;
		activeCompositions.put(composition.getCompositionUUID(), composition);
		compositionTransformer.install(composition);
	}
	
	public void onStopCompositionSession() {
		if (compositionSession != null) {
			UUID compositionUUID = compositionSession.getCompositionUUID();
			GSComposition composition = activeCompositions.remove(compositionUUID);
			if (composition != null)
				compositionTransformer.uninstall(composition);
			
			compositionSession = null;
		}
	}
	
	public void onCompositionSessionChanged(GSCompositionSession session) {
		manager.sendPacket(new GSCompositionSessionChangedPacket(session));
	}

	public void onStartSequenceSession(GSSequenceSession session) {
		GSComposition composition = activeCompositions.get(session.getCompositionUUID());
		if (composition != null) {
			GSTrack track = composition.getTrack(session.getTrackUUID());
			
			if (track != null) {
				this.sequenceSession = session;
				GSSequence sequence = track.getSequence();
				activeSequences.put(sequence.getSequenceUUID(), sequence);
				sequenceTransformer.install(sequence);

				openSequenceEditor();
			}
		}
	}
	
	public void openSequenceEditor() {
		GSSequence sequence = getSessionSequence();
		
		if (sequenceSession != null && sequence != null) {
			GSContentHistoryGUI primaryGUI = GSClientController.getInstance().getPrimaryGUI();
			primaryGUI.setContent(new GSSequenceEditPanel(this, sequenceSession, getSessionSequence()));
		}
	}
	
	public void onStopSequenceSession() {
		if (sequenceSession != null) {
			UUID sequenceUUID = sequenceSession.getSequenceUUID();
			GSSequence sequence = activeSequences.remove(sequenceUUID);
			if (sequence != null)
				sequenceTransformer.uninstall(sequence);

			sequenceSession = null;
		}
	}
	
	public void onSequenceSessionChanged(GSSequenceSession session) {
		manager.sendPacket(new GSSequenceSessionChangedPacket(session));
	}

	public void requestSequenceSession(UUID trackUUID) {
		manager.sendPacket(new GSSequenceSessionRequestPacket(trackUUID));
	}

	public GSCompositionSession getCompositionSession() {
		return compositionSession;
	}
	
	public GSSequenceSession getSequenceSession() {
		return sequenceSession;
	}
	
	public GSComposition getSessionComposition() {
		if (compositionSession != null)
			return getComposition(compositionSession.getCompositionUUID());
		return null;
	}
	
	public GSComposition getComposition(UUID compositionUUID) {
		return activeCompositions.get(compositionUUID);
	}

	public GSSequence getSessionSequence() {
		if (sequenceSession != null)
			return getSequence(sequenceSession.getSequenceUUID());
		return null;
	}

	public GSSequence getSequence(UUID sequenceUUID) {
		return activeSequences.get(sequenceUUID);
	}
	
	@Override
	public void onSequenceDelta(GSISequenceDelta delta) {
		if (sequenceSession != null)
			sequenceSession.trackSequenceDelta(delta);
	}
	
	@Override
	public void onCompositionDelta(GSICompositionDelta delta) {
		if (compositionSession != null)
			manager.sendPacket(new GSCompositionDeltaPacket(compositionSession.getCompositionUUID(), delta));
	}
	
	public void onDeltaReceived(UUID compositionUUID, GSICompositionDelta delta) {
		GSComposition composition = getComposition(compositionUUID);
		
		if (composition != null) {
			try {
				setTransformerEnabled(false);
				delta.applyDelta(composition);
			} catch (GSCompositionDeltaException ignore) {
			} finally {
				setTransformerEnabled(true);
			}
		}
	}

	public void onCompositionReset(GSComposition expectedComposition) {
		GSComposition composition = getComposition(expectedComposition.getCompositionUUID());
		
		if (composition != null) {
			try {
				setTransformerEnabled(false);
				composition.set(expectedComposition);
			} finally {
				setTransformerEnabled(true);
			}
		}
	}
	
	private void setTransformerEnabled(boolean enabled) {
		compositionTransformer.setEnabled(enabled);
		sequenceTransformer.setEnabled(enabled);
	}
}
