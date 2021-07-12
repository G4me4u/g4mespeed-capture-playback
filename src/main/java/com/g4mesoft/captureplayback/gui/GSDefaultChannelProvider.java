package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.panel.composition.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.module.translation.GSTranslationModule;
import com.g4mesoft.util.GSColorUtil;

import net.minecraft.util.math.BlockPos;

public class GSDefaultChannelProvider implements GSIChannelProvider {

	private static final String DEFAULT_CHANNEL_NAME = "panel.sequence.defaultname";
	private static final int MAX_COLOR_TRIES = 10;
	
	private static String defaultChannelName;
	
	@Override
	public GSChannelInfo createNextChannelInfo(GSSequence sequence) {
		return new GSChannelInfo(getDefaultChannelName(), 
		                         getUniqueColor(sequence),
		                         getChannelPosition());
	}

	public static String getDefaultChannelName() {
		GSTranslationModule translationModule = GSClientController.getInstance().getTranslationModule();
		if (defaultChannelName == null && translationModule.hasTranslation(DEFAULT_CHANNEL_NAME))
			defaultChannelName = translationModule.getTranslation(DEFAULT_CHANNEL_NAME);
		return defaultChannelName;
	}

	public static int getUniqueColor(GSSequence sequence) {
		return getUniqueColor(sequence, MAX_COLOR_TRIES);
	}

	public static int getUniqueColor(GSSequence sequence, int maxTries) {
		int color;

		do {
			color = (int)(Math.random() * 0xFFFFFF);
			if (isColorUnique(sequence, color))
				break;
		} while (maxTries-- <= 0);

		return color;
	}

	private static boolean isColorUnique(GSSequence sequence, int color) {
		for (GSChannel channel : sequence.getChannels()) {
			if (GSColorUtil.isRGBSimilar(channel.getInfo().getColor(), color))
				return false;
		}

		return true;
	}
	
	private static BlockPos getChannelPosition() {
		BlockPos position = GSCapturePlaybackClientModule.getCrosshairTarget();
		return (position == null) ? BlockPos.ORIGIN : position;
	}
}
