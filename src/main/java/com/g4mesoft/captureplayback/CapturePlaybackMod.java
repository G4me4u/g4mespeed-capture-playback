package com.g4mesoft.captureplayback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.g4mesoft.G4mespeedMod;
import com.g4mesoft.core.GSCoreOverride;

import net.fabricmc.api.ModInitializer;

public class CapturePlaybackMod implements ModInitializer {

	public static final String MOD_NAME = "G4mespeed Capture & Playback";
	public static final byte EXTENSION_UID = 0x04;
	
	public static final Logger GSP_LOGGER = LogManager.getLogger(MOD_NAME);

	private static CapturePlaybackMod instance;
	
	private GSCapturePlaybackExtension extension;
	
	@Override
	@GSCoreOverride
	public void onInitialize() {
		instance = this;
		
		extension = new GSCapturePlaybackExtension();
		G4mespeedMod.addExtension(extension);

		GSP_LOGGER.info(MOD_NAME + " initialized!");
	}
	
	public GSCapturePlaybackExtension getExtension() {
		return extension;
	}
	
	public static CapturePlaybackMod getInstance() {
		return instance;
	}
}
