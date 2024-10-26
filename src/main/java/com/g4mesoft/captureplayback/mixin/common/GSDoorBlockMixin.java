package com.g4mesoft.captureplayback.mixin.common;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

@Mixin(DoorBlock.class)
public class GSDoorBlockMixin {

	@Inject(
		method = "getPlacementState",
		require = 2,
		allow = 2,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/World;isReceivingRedstonePower(" +
					"Lnet/minecraft/util/math/BlockPos;" +
				")Z"
		)
	)
	private void onGetPlacementStateBeforePowerCheck(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
		// Only get power from play-back during first power check.
		((GSIWorldAccess)ctx.getWorld()).gcp_requestPlaybackPower(1);
	}

	@Inject(
		method = "neighborUpdate",
		require = 2,
		allow = 2,
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
		// Only get power from play-back during first power check.
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}
}
