package com.g4mesoft.captureplayback.access;

import net.minecraft.core.BlockPos;

public interface GSISignalGetterAccess {

	public void gcp_requestPlaybackPower(int callCount);
	
	public boolean gcp_fulfillPlaybackPowerRequest();
	
	public boolean gcp_isPoweredByPlayback(BlockPos pos);
	
}
