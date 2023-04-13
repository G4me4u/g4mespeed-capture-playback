package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(TrapdoorBlock.class)
public class GSTrapdoorBlockMixin {

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
	private void onNeighborUpdateBeforePowerCheck(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}

	@Inject(
		method = "getPlacementState",
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
	private void onGetPlacementStateBeforePowerCheck(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
		((GSIWorldAccess)ctx.getWorld()).gcp_requestPlaybackPower(1);
	}
}
