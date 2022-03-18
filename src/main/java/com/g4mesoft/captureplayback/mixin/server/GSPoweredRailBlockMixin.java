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
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(PoweredRailBlock.class)
public class GSPoweredRailBlockMixin {

	@Inject(method = "isPoweredByOtherRails(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ZILnet/minecraft/block/enums/RailShape;)Z",
			allow = 1, at = @At(value = "INVOKE", shift = Shift.BEFORE, target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
	private void onIsPoweredByOtherRailsBeforePowerCheck(World world, BlockPos pos, boolean bl, int distance, RailShape shape, CallbackInfoReturnable<Boolean> cir) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}

	@Inject(method = "updateBlockState", allow = 1, at = @At(value = "INVOKE", shift = Shift.BEFORE,
			target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
	private void onUpdateBlockStateBeforePowerCheck(BlockState state, World world, BlockPos pos, Block neighbor, CallbackInfo ci) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}
}
