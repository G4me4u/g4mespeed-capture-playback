package com.g4mesoft.captureplayback.mixin.common;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.g4mesoft.captureplayback.access.GSILevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

@Mixin(BellBlock.class)
public class GSBellBlockMixin {

	@Inject(
		method = "neighborChanged",
		allow = 1,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/level/Level;hasNeighborSignal(" +
					"Lnet/minecraft/core/BlockPos;" +
				")Z"
		)
	)
	private void onNeighborUpdateBeforePowerCheck(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation orientation, boolean notify, CallbackInfo ci) {
		((GSILevelAccess)world).gcp_requestPlaybackPower(1);
	}
}
