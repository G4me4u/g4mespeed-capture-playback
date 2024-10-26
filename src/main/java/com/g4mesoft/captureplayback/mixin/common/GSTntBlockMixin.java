package com.g4mesoft.captureplayback.mixin.common;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

@Mixin(TntBlock.class)
public class GSTntBlockMixin {

	@Inject(
		method = "onBlockAdded",
		allow = 1,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/World;isReceivingRedstonePower(" +
					"Lnet/minecraft/util/math/BlockPos;" +
				")Z"
		)
	)
	private void onOnBlockAddedBeforePowerCheck(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}

	@Inject(
		method = "neighborUpdate",
		allow = 1,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/World;isReceivingRedstonePower(" +
					"Lnet/minecraft/util/math/BlockPos;" +
				")Z"
		)
	)
	private void onNeighborUpdateBeforePowerCheck(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}
}
