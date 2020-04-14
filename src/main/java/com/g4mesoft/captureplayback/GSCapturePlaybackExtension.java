package com.g4mesoft.captureplayback;

import com.g4mesoft.GSIExtension;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.packet.GSPacketRegistry;

public class GSCapturePlaybackExtension implements GSIExtension {

	private static final String TRANSLATION_PATH = "/assets/g4mespeed/captureplayback/lang/en.lang";
	
	@Override
	public void registerPackets(GSPacketRegistry registry) {
	}
	
	@Override
	public void addClientModules(GSControllerClient controller) {
		controller.addModule(new GSCapturePlaybackModule());
	}

	@Override
	public void addServerModules(GSControllerServer controller) {
		controller.addModule(new GSCapturePlaybackModule());
	}
	
	@Override
	public String getTranslationPath() {
		return TRANSLATION_PATH;
	}

	@Override
	public String getName() {
		return G4mespeedCapturePlaybackMod.MOD_NAME;
	}

	@Override
	public byte getUniqueId() {
		return G4mespeedCapturePlaybackMod.EXTENSION_UID;
	}
}
