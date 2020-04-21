package com.g4mesoft.captureplayback;

import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.module.GSTimelineDeltaPacket;
import com.g4mesoft.captureplayback.module.GSTimelinePacket;
import com.g4mesoft.captureplayback.timeline.delta.GSEntryAddedDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSEntryRemovedDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSEntryTimeDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSEntryTypeDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSITimelineDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSTimelineNameDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSTrackAddedDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSTrackDisabledDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSTrackInfoDelta;
import com.g4mesoft.captureplayback.timeline.delta.GSTrackRemovedDelta;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.registry.GSElementRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class GSCapturePlaybackExtension implements GSIExtension {

	private static final String TRANSLATION_PATH = "/assets/g4mespeed/captureplayback/lang/en.lang";
	
	private GSElementRegistry<GSITimelineDelta> deltaRegistry;
	
	@Environment(EnvType.CLIENT)
	private GSCapturePlaybackModule clientModule;
	private GSCapturePlaybackModule serverModule;
	
	@Override
	public void init() {
		deltaRegistry = new GSElementRegistry<GSITimelineDelta>();
		
		deltaRegistry.register(0, GSTimelineNameDelta.class, GSTimelineNameDelta::new);
		deltaRegistry.register(1, GSTrackAddedDelta.class, GSTrackAddedDelta::new);
		deltaRegistry.register(2, GSTrackRemovedDelta.class, GSTrackRemovedDelta::new);
		deltaRegistry.register(3, GSTrackInfoDelta.class, GSTrackInfoDelta::new);
		deltaRegistry.register(4, GSTrackDisabledDelta.class, GSTrackDisabledDelta::new);
		deltaRegistry.register(5, GSEntryAddedDelta.class, GSEntryAddedDelta::new);
		deltaRegistry.register(6, GSEntryRemovedDelta.class, GSEntryRemovedDelta::new);
		deltaRegistry.register(7, GSEntryTimeDelta.class, GSEntryTimeDelta::new);
		deltaRegistry.register(8, GSEntryTypeDelta.class, GSEntryTypeDelta::new);
	}
	
	@Override
	public void registerPackets(GSElementRegistry<GSIPacket> registry) {
		registry.register(0, GSTimelinePacket.class, GSTimelinePacket::new);
		registry.register(1, GSTimelineDeltaPacket.class, GSTimelineDeltaPacket::new);
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
	public String getName() {
		return CapturePlaybackMod.MOD_NAME;
	}

	@Override
	public byte getUniqueId() {
		return CapturePlaybackMod.EXTENSION_UID;
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
	
	public GSElementRegistry<GSITimelineDelta> getDeltaRegistry() {
		return deltaRegistry;
	}
}
