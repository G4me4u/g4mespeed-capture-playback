package com.g4mesoft.captureplayback.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(World.class)
public class GSWorldMixin implements GSIWorldAccess {

	private static final int FULL_POWER_VALUE = 15;
	
	@Inject(method = "getEmittedRedstonePower", cancellable = true, at = @At("HEAD"))
	private void onGetEmittedRedstonePower(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (isPoweredByPlayback(pos.offset(direction.getOpposite()))) {
			cir.setReturnValue(FULL_POWER_VALUE);
			cir.cancel();
		}
	}
	
	@Override
	public boolean isPoweredByPlayback(BlockPos pos) {
		return false;
	}
}
