package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSISignalGetterAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

@Mixin(PistonBaseBlock.class)
public class GSPistonBaseBlockMixin {
	
	@Inject(
		method = "getNeighborSignal",
		at = @At(
			value = "INVOKE",
			ordinal = 0,
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/level/SignalGetter;hasSignal(" +
					"Lnet/minecraft/core/BlockPos;" +
					"Lnet/minecraft/core/Direction;" +
				")Z"
		)
	)
	private void onGetNeighborSignalBeforePowerCheck0(SignalGetter world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
		// Only request play-back power on the initial check around the piston itself.
		((GSISignalGetterAccess)world).gcp_requestPlaybackPower(1);
	}
}
