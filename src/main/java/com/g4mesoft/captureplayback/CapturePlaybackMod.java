package com.g4mesoft.captureplayback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.g4mesoft.core.GSCoreOverride;

import net.fabricmc.api.ModInitializer;

public class CapturePlaybackMod implements ModInitializer {

	public static final Logger GSCP_LOGGER = LogManager.getLogger("G4mespeed Capture & Playback");

	@Override
	@GSCoreOverride
	public void onInitialize() {
		GSCP_LOGGER.info("Capture & Playback {} initialized!", GSCapturePlaybackExtension.VERSION);
	}
}
