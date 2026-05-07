package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSISignalGetterAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;

@Mixin(SignalGetter.class)
public interface GSSignalGetterMixin extends GSISignalGetterAccess {

	@Inject(
		method =
			"getControlInputSignal(" +
				"Lnet/minecraft/core/BlockPos;" +
				"Lnet/minecraft/core/Direction;" +
				"Z" +
			")I",
		cancellable = true,
		at = @At("HEAD")
	)
	default public void onGetControlInputSignal(BlockPos pos, Direction direction, boolean onlyFromGate, CallbackInfoReturnable<Integer> cir) {
		if (gcp_fulfillPlaybackPowerRequest()) {
			if (gcp_isPoweredByPlayback(pos.relative(direction.getOpposite())))
				cir.setReturnValue(15);
		}
	}

	@Inject(
		method =
			"getSignal(" +
				"Lnet/minecraft/core/BlockPos;" +
				"Lnet/minecraft/core/Direction;" +
			")I",
		cancellable = true,
		at = @At("HEAD")
	)
	default public void onGetSignal(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (gcp_fulfillPlaybackPowerRequest()) {
			if (gcp_isPoweredByPlayback(pos.relative(direction.getOpposite())))
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
