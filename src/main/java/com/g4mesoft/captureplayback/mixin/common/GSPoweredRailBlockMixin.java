package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSILevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

@Mixin(PoweredRailBlock.class)
public class GSPoweredRailBlockMixin {

	@Inject(
		method =
			"isSameRailWithPower(" +
				"Lnet/minecraft/world/level/Level;" +
				"Lnet/minecraft/core/BlockPos;" +
				"ZI" +
				"Lnet/minecraft/world/level/block/state/properties/RailShape;" +
			")Z",
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
	private void onIsSameRailWithPowerBeforePowerCheck(Level world, BlockPos pos, boolean bl, int distance, RailShape shape, CallbackInfoReturnable<Boolean> cir) {
		((GSILevelAccess)world).gcp_requestPlaybackPower(1);
	}

	@Inject(
		method = "updateState",
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
	private void onUpdateStateBeforePowerCheck(BlockState state, Level world, BlockPos pos, Block neighbor, CallbackInfo ci) {
		((GSILevelAccess)world).gcp_requestPlaybackPower(1);
	}
}
