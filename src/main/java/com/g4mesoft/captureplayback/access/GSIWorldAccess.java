package com.g4mesoft.captureplayback.access;

import net.minecraft.util.math.BlockPos;

public interface GSIWorldAccess {

	public void requestPlaybackPower(int callCount);

	public boolean isPoweredByPlayback(BlockPos pos);
	
}
