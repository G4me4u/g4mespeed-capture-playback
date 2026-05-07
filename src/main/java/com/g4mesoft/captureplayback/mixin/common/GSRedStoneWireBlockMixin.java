package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSILevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;

@Mixin(RedStoneWireBlock.class)
public class GSRedStoneWireBlockMixin {

	@Inject(
		method = "getBlockSignal",
		allow = 1,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/level/Level;getBestNeighborSignal(" +
					"Lnet/minecraft/core/BlockPos;" +
				")I"
		)
	)
	private void onGetBlockSignalBeforePowerCheck(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		((GSILevelAccess)level).gcp_requestPlaybackPower(1);
	}
}
