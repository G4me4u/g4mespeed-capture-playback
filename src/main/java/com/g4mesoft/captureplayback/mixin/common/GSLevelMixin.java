package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import com.g4mesoft.captureplayback.access.GSILevelAccess;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public abstract class GSLevelMixin implements GSILevelAccess {

	private int gcp_powerRequests;
	
	@Override
	public boolean gcp_fulfillPlaybackPowerRequest() {
		if (gcp_powerRequests > 0) {
			gcp_powerRequests--;
			return true;
		}
		return false;
	}
	
	@Override
	public void gcp_requestPlaybackPower(int callCount) {
		if (callCount > 0)
			gcp_powerRequests += callCount;
	}
}
