package com.g4mesoft.captureplayback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.g4mesoft.G4mespeedMod;
import com.g4mesoft.core.GSCoreOverride;

import net.fabricmc.api.ModInitializer;

public class CapturePlaybackMod implements ModInitializer {

	public static final Logger GSCP_LOGGER = LogManager.getLogger("G4mespeed Capture & Playback");

	private static CapturePlaybackMod instance;
	
	private GSCapturePlaybackExtension extension;
	
	@Override
	@GSCoreOverride
	public void onInitialize() {
		instance = this;
		
		extension = new GSCapturePlaybackExtension();
		G4mespeedMod.addExtension(extension);

		GSCP_LOGGER.info("Capture & Playback " + GSCapturePlaybackExtension.VERSION + " initialized!");
	}
	
	public GSCapturePlaybackExtension getExtension() {
		return extension;
	}
	
	public static CapturePlaybackMod getInstance() {
		return instance;
	}
}
