package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.panel.composition.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.ui.panel.GSPanelContext;
import com.g4mesoft.ui.util.GSColorUtil;

import net.minecraft.util.math.BlockPos;

public class GSDefaultChannelProvider implements GSIChannelProvider {

	private static final String DEFAULT_CHANNEL_NAME = "panel.sequence.defaultname";
	private static final int MAX_COLOR_TRIES = 10;
	
	private static String defaultChannelName = null;
	
	@Override
	public GSChannelInfo createChannelInfo(GSSequence sequence) {
		BlockPos pos = GSCapturePlaybackClientModule.getCrosshairTarget();
		return createChannelInfo(sequence, (pos == null) ? BlockPos.ORIGIN : pos);
	}

	public GSChannelInfo createChannelInfo(GSSequence sequence, BlockPos pos) {
		return new GSChannelInfo(getDefaultChannelName(), getUniqueColor(sequence), pos);
	}

	private static String getDefaultChannelName() {
		if (defaultChannelName == null)
			defaultChannelName = GSPanelContext.i18nTranslate(DEFAULT_CHANNEL_NAME);
		return defaultChannelName;
	}

	public static int getUniqueColor(GSSequence sequence) {
		return getUniqueColor(sequence, MAX_COLOR_TRIES);
	}

	private static int getUniqueColor(GSSequence sequence, int maxTries) {
		int color;
		do {
			color = (int)(Math.random() * 0xFFFFFF);
		} while (!isColorUnique(sequence, color) && maxTries-- <= 0);
		return color;
	}

	private static boolean isColorUnique(GSSequence sequence, int color) {
		for (GSChannel channel : sequence.getChannels()) {
			if (GSColorUtil.isRGBSimilar(channel.getInfo().getColor(), color))
				return false;
		}
		return true;
	}
}
