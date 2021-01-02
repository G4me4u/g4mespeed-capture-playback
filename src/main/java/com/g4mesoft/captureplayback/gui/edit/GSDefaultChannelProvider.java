package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.util.GSColorUtil;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.module.translation.GSTranslationModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class GSDefaultChannelProvider implements GSIChannelProvider {

	private static final String DEFAULT_CHANNEL_NAME = "sequence.channel.defaultname";
	private static final int MAX_COLOR_TRIES = 10;
	
	@Override
	public GSChannelInfo createNextChannelInfo(GSSequence sequence) {
		GSTranslationModule translationModule = GSControllerClient.getInstance().getTranslationModule();
		String channelName = translationModule.getTranslation(DEFAULT_CHANNEL_NAME);
		return new GSChannelInfo(channelName, getNextChannelPos(), getUniqueColor(sequence, MAX_COLOR_TRIES));
	}

	private BlockPos getNextChannelPos() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK)
			return ((BlockHitResult)client.crosshairTarget).getBlockPos();
		return new BlockPos(0, 0, 0);
	}
	
	private int getUniqueColor(GSSequence sequence, int maxTries) {
		int color = 0x000000;

		int tries = 0;
		while (++tries <= maxTries) {
			color = (int)(Math.random() * 0xFFFFFF);

			if (isColorUnique(sequence, color))
				break;
		}

		return color;
	}

	private boolean isColorUnique(GSSequence sequence, int color) {
		for (GSChannel channel : sequence.getChannels()) {
			if (GSColorUtil.isRGBSimilar(channel.getInfo().getColor(), color))
				return false;
		}

		return true;
	}
}
