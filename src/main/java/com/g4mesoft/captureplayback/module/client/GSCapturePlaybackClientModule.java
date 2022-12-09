package com.g4mesoft.captureplayback.module.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.asset.GSAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.common.asset.GSUnmodifiableAssetHistory;
import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.gui.GSCompositionEditPanel;
import com.g4mesoft.captureplayback.gui.GSDefaultChannelProvider;
import com.g4mesoft.captureplayback.gui.GSSequenceEditPanel;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSESessionRequestType;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSISessionListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSSessionDeltasPacket;
import com.g4mesoft.captureplayback.session.GSSessionRequestPacket;
import com.g4mesoft.captureplayback.session.GSSessionSide;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.client.GSIClientModule;
import com.g4mesoft.core.client.GSIClientModuleManager;
import com.g4mesoft.gui.GSTabbedGUI;
import com.g4mesoft.hotkey.GSEKeyEventType;
import com.g4mesoft.hotkey.GSKeyManager;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.setting.GSSettingCategory;
import com.g4mesoft.setting.GSSettingManager;
import com.g4mesoft.setting.types.GSIntegerSetting;
import com.g4mesoft.util.GSColorUtil;

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
	
	private final Map<UUID, GSSession> sessions;
	private final Map<GSESessionType, GSSession> sessionByType;
	private final Map<GSESessionType, GSPanel> sessionPanels;
	private final GSDefaultChannelProvider channelProvider;
	
	private final GSIAssetHistory assetHistory;
	private GSIAssetHistory unmodifiableAssetHistory;
	
	private GSIClientModuleManager manager;
	
	public final GSIntegerSetting cChannelRenderingType;
	
	public GSCapturePlaybackClientModule() {
		sessions = new HashMap<>();
		sessionByType = new EnumMap<>(GSESessionType.class);
		sessionPanels = new EnumMap<>(GSESessionType.class);
		channelProvider = new GSDefaultChannelProvider();
		
		assetHistory = new GSAssetHistory();
		
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
		keyManager.registerKey("channelRenderingType", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN,
				cChannelRenderingType::incrementValue, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("newChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::createNewChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("extendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::extendChannel, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("unextendChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, this::unextendChannel, GSEKeyEventType.PRESS);

		keyManager.registerKey("selectChannel", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, () -> {
			GSSession session = getSession(GSESessionType.SEQUENCE);
			GSChannel channel = getChannelAtCrosshair(session);
			if (channel != null && session != null)
				session.set(GSSession.SELECTED_CHANNEL, channel.getChannelUUID());
		}, GSEKeyEventType.PRESS);
		
		keyManager.registerKey("pasteChannelColor", KEY_CATEGORY, GLFW.GLFW_KEY_UNKNOWN, channel -> {
			GSSession session = getSession(GSESessionType.SEQUENCE);
			
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
		GSSession session = getSession(GSESessionType.SEQUENCE);
		
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
		GSSession session = getSession(GSESessionType.SEQUENCE);
		
		if (session != null) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			BlockPos position = getCrosshairTarget();
			GSChannel channel = sequence.getChannel(session.get(GSSession.SELECTED_CHANNEL));
			
			if (position != null && channel != null)
				channel.setInfo(channel.getInfo().addPosition(position));
		}
	}

	private void unextendChannel() {
		GSSession session = getSession(GSESessionType.SEQUENCE);
		
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
		GSChannel channel = getChannelAtCrosshair(getSession(GSESessionType.SEQUENCE));
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
	
	public GSSession getSession(GSESessionType sessionType) {
		return sessionByType.get(sessionType);
	}
	
	@Override
	public void registerClientSettings(GSSettingManager settings) {
		settings.registerSetting(CAPTURE_PLAYBACK_CATEGORY, cChannelRenderingType);
	}

	public GSIAssetHistory getAssetHistory() {
		if (unmodifiableAssetHistory == null)
			unmodifiableAssetHistory = new GSUnmodifiableAssetHistory(assetHistory);
		return unmodifiableAssetHistory;
	}
	
	@Override
	public void onDisconnectServer() {
		UUID[] assetUUIDs = sessions.keySet().toArray(new UUID[0]);
		for (UUID assetUUID : assetUUIDs)
			onSessionStop(assetUUID);
	}

	public void onSessionStart(GSSession session) {
		GSSession activeSession = getSession(session.getType());
		if (activeSession != null) {
			// Notify the server that we are stopping the active session
			UUID assetUUID = activeSession.get(GSSession.ASSET_UUID);
			requestSession(GSESessionRequestType.REQUEST_STOP, assetUUID);
			onSessionStop(assetUUID);
		}
		
		sessions.put(session.get(GSSession.ASSET_UUID), session);
		sessionByType.put(session.getType(), session);
		session.setSide(GSSessionSide.CLIENT_SIDE);
		session.addListener(this);
	
		switch (session.getType()) {
		case COMPOSITION:
			openSessionPanel(session.getType(), new GSCompositionEditPanel(session));
			break;
		case SEQUENCE:
			openSessionPanel(session.getType(), new GSSequenceEditPanel(session, channelProvider));
			break;
		}
	}

	public void onSessionStop(UUID assetUUID) {
		GSSession session = sessions.remove(assetUUID);
		if (session != null) {
			sessionByType.remove(session.getType());
			session.removeListener(this);
			closeSessionPanel(session.getType());
		}
	}
	
	private void openSessionPanel(GSESessionType sessionType, GSPanel panel) {
		if (sessionPanels.containsKey(sessionType))
			closeSessionPanel(sessionType);
		sessionPanels.put(sessionType, panel);
		
		GSClientController.getInstance().getPrimaryGUI().setContent(panel);
	}
	
	private void closeSessionPanel(GSESessionType sessionType) {
		GSPanel panel = sessionPanels.remove(sessionType);
		if (panel != null)
			GSClientController.getInstance().getPrimaryGUI().removeHistory(panel);
	}

	public void requestSession(GSESessionRequestType requestType, UUID assetUUID) {
		manager.sendPacket(new GSSessionRequestPacket(requestType, assetUUID));
	}
	
	@Override
	public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
		manager.sendPacket(new GSSessionDeltasPacket(session.get(GSSession.ASSET_UUID), deltas));
	}
	
	public void onSessionDeltasReceived(UUID assetUUID, GSIDelta<GSSession>[] deltas) {
		GSSession session = sessions.get(assetUUID);
		if (session != null)
			session.applySessionDeltas(deltas);
	}

	public void onAssetHistoryReceived(GSAssetHistory history) {
		assetHistory.set(history);
	}

	public void onAssetInfoChanged(GSAssetInfo info) {
		assetHistory.add(info);
	}

	public void onAssetInfoRemoved(UUID assetUUID) {
		assetHistory.remove(assetUUID);
	}
}
