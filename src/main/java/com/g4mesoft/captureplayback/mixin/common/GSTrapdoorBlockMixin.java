package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSILevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(TrapDoorBlock.class)
public class GSTrapdoorBlockMixin {

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
	private void onNeighborChangedBeforePowerCheck(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		((GSILevelAccess)world).gcp_requestPlaybackPower(1);
	}

	@Inject(
		method = "getStateForPlacement",
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
	private void onGetStateForPlacementBeforePowerCheck(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
		((GSILevelAccess)ctx.getLevel()).gcp_requestPlaybackPower(1);
	}
}
