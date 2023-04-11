package com.g4mesoft.captureplayback.module.client;

import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.gui.GSDefaultChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.client.GSIClientModule;
import com.g4mesoft.core.client.GSIClientModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSEKeyEventType;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.setting.GSSettingCategory;
import com.g4mesoft.setting.GSSettingManager;
import com.g4mesoft.setting.types.GSIntegerSetting;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.util.GSColorUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class GSCapturePlaybackClientModule implements GSIClientModule, GSISessionListener {

	public static final int RENDERING_DISABLED = 0;
	public static final int RENDERING_DEPTH    = 1;
	public static final int RENDERING_NO_DEPTH = 2;
	
	private static final String GUI_TAB_TITLE = "gui.tab.capture-playback";
	public static final String KEY_CATEGORY = "capture-playback";
	public static final GSSettingCategory CAPTURE_PLAYBACK_CATEGORY = new GSSettingCategory("capture-playback");
	
	private GSClientAssetManager assetManager;
	private final GSDefaultChannelProvider channelProvider;
	
	public final GSIntegerSetting cChannelRenderingType;
	
	public GSCapturePlaybackClientModule() {
		assetManager = null;
		channelProvider = new GSDefaultChannelProvider();
		
		cChannelRenderingType = new GSIntegerSetting("channelRenderingType", RENDERING_DISABLED, 0, 2);
	}
	
	@Override
	public void init(GSIClientModuleManager manager) {
		assetManager = new GSClientAssetManager(manager);
		
		manager.addRenderable(new GSSequencePositionRenderable(this));
	}
	
	@Override
	public void onClose() {
		assetManager = null;
	}
	
	@Override
	public void initGUI(GSTabbedGUI tabbedGUI) {
		tabbedGUI.addTab(GUI_TAB_TITLE, new GSScrollPanel(new GSCapturePlaybackPanel(assetManager)));
	}
	
	@Override
	public void registerHotkeys(GSKeyManager keyManager) {
		keyManager.registerKey("channelRenderingType", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN,
				cChannelRenderingType::increment, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("newChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::createNewChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("extendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::extendChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("unextendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::unextendChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("selectChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
			GSChannel channel = getChannelAtCrosshair(session);
			if (channel != null && session != null)
				session.set(GSSession.SELECTED_CHANNEL, channel.getChannelUUID());
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("pasteChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
			
			if (session != null) {
				GSSequence sequence = session.get(GSSession.SEQUENCE);
				GSChannel selectedChannel = sequence.getChannel(session.get(GSSession.SELECTED_CHANNEL));
				if (selectedChannel != null)
					return channel.getInfo().withColor(selectedChannel.getInfo().getColor());
			}
			return channel.getInfo();
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("brightenChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSColorUtil.brighter(channel.getInfo().getColor()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("darkenChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSColorUtil.darker(channel.getInfo().getColor()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("randomizeChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			return channel.getInfo().withColor(GSDefaultChannelProvider.getUniqueColor(channel.getParent()));
		}, this::modifyCrosshairChannel, GSEKeyEventType.PRESS);
	}
	
	private void createNewChannel() {
		GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
		
		if (session != null) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			BlockPos position = getCrosshairTarget();
			// Only add channels if we have a crosshair target
			if (position != null) {
				GSChannelInfo info = channelProvider.createChannelInfo(sequence, position);
				
				GSChannel channel = sequence.addChannel(info);
				// Automatically select the new channel
				if (channel != null && session != null)
					session.set(GSSession.SELECTED_CHANNEL, channel.getChannelUUID());
			}
		}
	}
	
	private void extendChannel() {
		GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
		
		if (session != null) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequence.getChannel(session.get(GSSession.SELECTED_CHANNEL));
			
			if (position != null && channel != null)
				channel.setInfo(channel.getInfo().addPosition(position));
		}
	}

	private void unextendChannel() {
		GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
		
		if (session != null) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequence.getChannel(session.get(GSSession.SELECTED_CHANNEL));
			
			if (position != null && channel != null) {
				GSChannelInfo info = channel.getInfo();
				// Ensure that we have at least one position in the channel
				if (info.getPositions().size() > 1)
					channel.setInfo(info.removePosition(position));
			}
		}
	}
	
	private GSChannel getChannelAtCrosshair(GSSession session) {
		if (session != null && session.getType() == GSESessionType.SEQUENCE) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			
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
		GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
		GSChannel channel = getChannelAtCrosshair(session);
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
		assetManager.onDisconnect();
	}

	public GSClientAssetManager getAssetManager() {
		return assetManager;
	}
}
