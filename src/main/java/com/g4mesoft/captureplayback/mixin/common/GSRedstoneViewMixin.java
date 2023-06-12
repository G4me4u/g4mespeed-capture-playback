package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIRedstoneViewAccess;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;

@Mixin(RedstoneView.class)
public interface GSRedstoneViewMixin extends GSIRedstoneViewAccess {

	@Inject(
		method =
			"getEmittedRedstonePower(" +
				"Lnet/minecraft/util/math/BlockPos;" +
				"Lnet/minecraft/util/math/Direction;" +
				"Z" +
			")I",
		cancellable = true,
		at = @At("HEAD")
	)
	default public void onGetEmittedRedstonePower(BlockPos pos, Direction direction, boolean onlyFromGate, CallbackInfoReturnable<Integer> cir) {
		if (gcp_fulfillPlaybackPowerRequest()) {
			if (gcp_isPoweredByPlayback(pos.offset(direction.getOpposite())))
				cir.setReturnValue(15);
		}
	}

	@Inject(
		method =
			"getEmittedRedstonePower(" +
				"Lnet/minecraft/util/math/BlockPos;" +
				"Lnet/minecraft/util/math/Direction;" +
			")I",
		cancellable = true,
		at = @At("HEAD")
	)
	default public void onGetEmittedRedstonePower(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (gcp_fulfillPlaybackPowerRequest()) {
			if (gcp_isPoweredByPlayback(pos.offset(direction.getOpposite())))
				cir.setReturnValue(15);
		}
	}
	
	@Override
	default void gcp_requestPlaybackPower(int callCount) {
		// Do nothing...
	}
	
	@Override
	default public boolean gcp_fulfillPlaybackPowerRequest() {
		return false;
	}
	
	@Override
	default public boolean gcp_isPoweredByPlayback(BlockPos pos) {
		return false;
	}
}
