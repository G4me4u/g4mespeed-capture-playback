package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.GSITrackProvider;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;
import com.g4mesoft.captureplayback.util.GSColorUtil;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.module.translation.GSTranslationModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class GSDefaultTrackProvider implements GSITrackProvider {

	private static final String DEFAULT_TRACK_NAME = "timeline.track.defaultname";
	private static final int MAX_COLOR_TRIES = 10;
	
	@Override
	public GSTrackInfo createNewTrackInfo(GSTimeline timeline) {
		GSTranslationModule translationModule = GSControllerClient.getInstance().getTranslationModule();
		String trackName = translationModule.getTranslation(DEFAULT_TRACK_NAME);
		return new GSTrackInfo(trackName, getNewTrackPos(), getUniqueColor(timeline, MAX_COLOR_TRIES));
	}

	private BlockPos getNewTrackPos() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK)
			return ((BlockHitResult)client.crosshairTarget).getBlockPos();
		return new BlockPos(0, 0, 0);
	}
	
	private int getUniqueColor(GSTimeline timeline, int maxTries) {
		int color = 0x000000;

		int tries = 0;
		while (++tries <= maxTries) {
			color = (int)(Math.random() * 0xFFFFFF);

			if (isColorUnique(timeline, color))
				break;
		}

		return color;
	}

	private boolean isColorUnique(GSTimeline timeline, int color) {
		for (GSTrack track : timeline.getTracks()) {
			if (GSColorUtil.isRGBSimilar(track.getInfo().getColor(), color))
				return false;
		}

		return true;
	}
}
