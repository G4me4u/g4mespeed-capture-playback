package com.g4mesoft.captureplayback.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PistonBlock.class)
public class GSPistonBlockMixin {
	
	@Inject(method = "shouldExtend", at = @At(value = "INVOKE", ordinal = 0, shift = Shift.BEFORE,
			target = "Lnet/minecraft/world/World;isEmittingRedstonePower(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
	private void onShouldExtendBeforePowerCheck(World world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
		// Only request play-back power on the initial check around the piston itself.
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}
}
