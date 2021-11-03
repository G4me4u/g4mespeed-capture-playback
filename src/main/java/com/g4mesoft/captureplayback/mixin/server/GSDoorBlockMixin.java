package com.g4mesoft.captureplayback.mixin.server;

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

@Mixin(DoorBlock.class)
public class GSDoorBlockMixin {

	@Inject(method = "getPlacementState", at = @At(value = "INVOKE", ordinal = 0, shift = Shift.BEFORE,
			target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
	private void onGetPlacementStateBeforePowerCheck0(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
		// Only get power from play-back during first power check.
		((GSIWorldAccess)ctx.getWorld()).requestPlaybackPower(1);
	}

	@Inject(method = "neighborUpdate", at = @At(value = "INVOKE", ordinal = 0, shift = Shift.BEFORE,
			target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
	private void onNeighborUpdateBeforePowerCheck0(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		// Only get power from play-back during first power check.
		((GSIWorldAccess)world).requestPlaybackPower(1);
	}
}
