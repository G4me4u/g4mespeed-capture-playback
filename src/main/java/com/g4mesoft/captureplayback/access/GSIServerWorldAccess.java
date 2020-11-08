package com.g4mesoft.captureplayback.access;

import com.g4mesoft.captureplayback.stream.GSPlaybackStream;

import net.minecraft.util.math.BlockPos;

public interface GSIServerWorldAccess {

	public void playStream(GSPlaybackStream playbackStream);

	public boolean isPlaybackPosition(BlockPos pos);
	
}
